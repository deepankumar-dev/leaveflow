import type { ReactNode } from 'react'
import type { Status } from '../api'

export const STATUS_LABEL: Record<Status, string> = {
  PENDING_MANAGER: 'Pending manager',
  ESCALATED: 'Escalated to HR',
  PENDING_HR: 'Pending HR',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
}

const STATUS_STYLE: Record<Status, string> = {
  PENDING_MANAGER: 'bg-amber-50 text-amber-700 ring-amber-200',
  ESCALATED: 'bg-orange-50 text-orange-700 ring-orange-200',
  PENDING_HR: 'bg-sky-50 text-sky-700 ring-sky-200',
  APPROVED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  REJECTED: 'bg-rose-50 text-rose-700 ring-rose-200',
  CANCELLED: 'bg-slate-100 text-slate-600 ring-slate-200',
}

export function StatusBadge({ status }: { status: Status }) {
  return (
    <span className={`inline-flex whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${STATUS_STYLE[status]}`}>
      {STATUS_LABEL[status]}
    </span>
  )
}

export function FlagBadge({ reason }: { reason?: string | null }) {
  return (
    <span
      title={reason ?? 'Team coverage conflict'}
      className="inline-flex items-center gap-1 whitespace-nowrap rounded-full bg-rose-50 px-2.5 py-0.5 text-xs font-medium text-rose-700 ring-1 ring-inset ring-rose-200"
    >
      ⚑ Conflict
    </span>
  )
}

export function PageHeader({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

export function Empty({ title, hint }: { title: string; hint?: string }) {
  return (
    <div className="card flex flex-col items-center px-6 py-14 text-center">
      <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-xl">🌴</div>
      <p className="font-medium text-slate-700">{title}</p>
      {hint && <p className="mt-1 max-w-sm text-sm text-slate-500">{hint}</p>}
    </div>
  )
}

export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return <p className="py-10 text-center text-sm text-slate-500">{label}</p>
}

export function ErrorBox({ error }: { error: unknown }) {
  const msg = error instanceof Error ? error.message : 'Something went wrong'
  return <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{msg}</div>
}

const fmt = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
const fmtTime = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Kolkata',
})

export const fmtDate = (d: string) => fmt.format(new Date(d + 'T00:00:00'))
export const fmtDateTime = (iso: string) => fmtTime.format(new Date(iso))
export const fmtRange = (from: string, to: string) => (from === to ? fmtDate(from) : `${fmtDate(from)} → ${fmtDate(to)}`)
export const fmtDays = (n: number | null | undefined) => (n == null ? '–' : Number.isInteger(n) ? String(n) : n.toFixed(1))
