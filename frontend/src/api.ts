// Typed client for the LeaveFlow API (mirrors docs/openapi.yaml). All authorization is enforced by the backend;
// the UI only hides things the caller cannot use.

export type Role = 'EMPLOYEE' | 'MANAGER' | 'HR'
export type Status = 'PENDING_MANAGER' | 'ESCALATED' | 'PENDING_HR' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

export interface User {
  id: number
  name: string
  email: string
  role: Role
  teamId: number | null
  teamName: string | null
  managerId: number | null
  managerName: string | null
  joinDate: string
  active: boolean
  mustChangePassword: boolean
}
export interface Step {
  id: number
  stage: 'MANAGER' | 'HR'
  assigneeId: number | null
  assigneeName: string | null
  delegatedFromName: string | null
  dueAt: string | null
  decision: 'PENDING' | 'APPROVED' | 'REJECTED' | 'ESCALATED'
  decidedAt: string | null
  comment: string | null
}
export interface LeaveRequest {
  id: number
  employeeId: number
  employeeName: string
  teamName: string | null
  leaveTypeCode: string
  leaveTypeName: string
  fromDate: string
  toDate: string
  days: number
  reason: string | null
  status: Status
  flagged: boolean
  flagReason: string | null
  createdAt: string
  lastActionAt: string
  steps: Step[]
  canApprove: boolean
  canCancel: boolean
}
export interface TimelineEvent {
  id: number
  action: string
  actorName: string
  fromStatus: Status | null
  toStatus: Status | null
  comment: string | null
  at: string
}
export interface Balance {
  leaveTypeCode: string
  leaveTypeName: string
  year: number
  entitled: number | null
  used: number | null
  pending: number | null
  available: number | null
  tracked: boolean
  explanation: string
}
export interface Preview {
  valid: boolean
  errors: string[]
  workingDays: number | null
  balanceAvailable: number | null
  balanceAfter: number | null
  balanceExplanation: string | null
  conflictFlagged: boolean
  conflictReason: string | null
  overlappingTeammates: string[]
  teamSize: number
}
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
export interface Notification {
  id: number
  message: string
  read: boolean
  createdAt: string
  relatedRequestId: number | null
}
export interface Delegation {
  id: number
  delegatorName: string
  delegateId: number
  delegateName: string
  fromDate: string
  toDate: string
  active: boolean
}
export interface Holiday {
  id: number
  date: string
  name: string
}
export interface CalendarEntry {
  requestId: number
  employeeId: number
  employeeName: string
  leaveTypeCode: string
  fromDate: string
  toDate: string
  status: Status
  flagged: boolean
}
export interface TeamCalendar {
  month: string
  teamId: number
  teamName: string
  teamSize: number
  entries: CalendarEntry[]
  holidays: Holiday[]
}
export interface Analytics {
  totalRequests: number
  byStatus: Record<string, number>
  byLeaveType: Record<string, number>
  flaggedCount: number
  escalatedCount: number
  avgDecisionHours: number
  monthlyTrend: { month: string; days: number; requests: number }[]
  teamLoad: { teamName: string; pending: number; approved: number; flagged: number }[]
}

export interface Directory {
  teams: { id: number; name: string; size: number }[]
  managers: { id: number; name: string; teamName: string | null }[]
}

export interface Forecast {
  scope: string
  teamSize: number
  thresholdPercent: number
  historicalAvgAway: number
  days: { date: string; workingDay: boolean; approvedAway: number; pendingAway: number; projectedAwayPercent: number }[]
}

export interface PersonInput {
  name: string
  email: string
  role: Role
  teamId: number | null
  managerId: number | null
  joinDate: string
  password?: string
  active?: boolean
}

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string, public details: string[] = []) {
    super(message)
  }
}

const TOKEN_KEY = 'leaveflow.token'
export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (t: string | null) => (t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY))

let onUnauthorized: () => void = () => {}
export const setUnauthorizedHandler = (fn: () => void) => (onUnauthorized = fn)

async function request<T>(method: string, url: string, body?: unknown): Promise<T> {
  const token = getToken()
  const res = await fetch(url, {
    method,
    headers: {
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  if (res.ok) {
    const text = await res.text()
    return (text ? JSON.parse(text) : undefined) as T
  }
  let payload: { code?: string; message?: string; details?: string[] } = {}
  try {
    payload = await res.json()
  } catch {
    /* non-JSON error body */
  }
  if (res.status === 401 && url !== '/api/auth/login') onUnauthorized()
  throw new ApiError(res.status, payload.code ?? 'ERROR', payload.message ?? `Request failed (${res.status})`, payload.details ?? [])
}

const qs = (params: Record<string, string | number | undefined>) => {
  const p = Object.entries(params).filter(([, v]) => v !== undefined && v !== '')
  return p.length ? '?' + p.map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`).join('&') : ''
}

export const api = {
  login: (email: string, password: string) =>
    request<{ token: string; user: User }>('POST', '/api/auth/login', { email, password }),
  config: () => request<{ demoMode: boolean; appName: string }>('GET', '/api/public/config'),
  me: () => request<User>('GET', '/api/auth/me'),
  changePassword: (currentPassword: string, newPassword: string) =>
    request<{ token: string; user: User }>('POST', '/api/auth/change-password', { currentPassword, newPassword }),
  people: () => request<User[]>('GET', '/api/admin/users'),
  createPerson: (p: PersonInput) => request<User>('POST', '/api/admin/users', p),
  updatePerson: (id: number, p: PersonInput) => request<User>('PUT', `/api/admin/users/${id}`, p),
  resetPassword: (id: number, password: string) => request<void>('POST', `/api/admin/users/${id}/reset-password`, { password }),
  createTeam: (name: string) => request<{ id: number; name: string; size: number }>('POST', '/api/admin/teams', { name }),
  balances: () => request<Balance[]>('GET', '/api/balances/me'),
  preview: (leaveTypeCode: string, fromDate: string, toDate: string) =>
    request<Preview>('POST', '/api/leaves/preview', { leaveTypeCode, fromDate, toDate }),
  apply: (leaveTypeCode: string, fromDate: string, toDate: string, reason: string) =>
    request<LeaveRequest>('POST', '/api/leaves', { leaveTypeCode, fromDate, toDate, reason }),
  mine: () => request<LeaveRequest[]>('GET', '/api/leaves/mine'),
  request: (id: number) => request<LeaveRequest>('GET', `/api/leaves/${id}`),
  timeline: (id: number) => request<TimelineEvent[]>('GET', `/api/leaves/${id}/timeline`),
  approve: (id: number, comment?: string) => request<LeaveRequest>('POST', `/api/leaves/${id}/approve`, { comment }),
  reject: (id: number, comment: string) => request<LeaveRequest>('POST', `/api/leaves/${id}/reject`, { comment }),
  cancel: (id: number, comment?: string) => request<LeaveRequest>('POST', `/api/leaves/${id}/cancel`, { comment }),
  managerRequests: (p: { status?: string; q?: string; size?: number } = {}) =>
    request<Page<LeaveRequest>>('GET', '/api/manager/requests' + qs({ size: 100, ...p })),
  hrRequests: (p: { status?: string; q?: string; size?: number } = {}) =>
    request<Page<LeaveRequest>>('GET', '/api/hr/requests' + qs({ size: 100, ...p })),
  calendar: (month: string, teamId?: number) => request<TeamCalendar>('GET', '/api/calendar/team' + qs({ month, teamId })),
  delegations: () => request<Delegation[]>('GET', '/api/delegations'),
  delegate: (delegateId: number, fromDate: string, toDate: string) =>
    request<Delegation>('POST', '/api/delegations', { delegateId, fromDate, toDate }),
  revokeDelegation: (id: number) => request<void>('DELETE', `/api/delegations/${id}`),
  forecast: (teamId?: number, days = 30) => request<Forecast>('GET', '/api/analytics/forecast' + qs({ teamId, days })),
  directory: () => request<Directory>('GET', '/api/directory'),
  analytics: () => request<Analytics>('GET', '/api/analytics/summary'),
  holidays: () => request<Holiday[]>('GET', '/api/holidays'),
  addHoliday: (date: string, name: string) => request<Holiday>('POST', '/api/holidays', { date, name }),
  notifications: () => request<Notification[]>('GET', '/api/notifications'),
  markRead: (id: number) => request<Notification>('POST', `/api/notifications/${id}/read`),
  simulateTimeout: (id: number) => request<LeaveRequest>('POST', `/api/demo/simulate-timeout/${id}`),
  resetSeed: () => request<{ message: string }>('POST', '/api/demo/reset-seed'),
}
