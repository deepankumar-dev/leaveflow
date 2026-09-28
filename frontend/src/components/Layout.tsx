import { createContext, useContext, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, type Role } from '../api'
import { useAuth } from '../auth'
import { RequestDrawer } from './RequestDrawer'
import { useToast } from './Toast'
import { fmtDateTime } from './ui'

const DrawerCtx = createContext<(id: number) => void>(() => {})
/** Opens the request side panel from anywhere in the app. */
export const useOpenRequest = () => useContext(DrawerCtx)

interface NavItem {
  to: string
  label: string
  roles: Role[]
}
const NAV: NavItem[] = [
  { to: '/inbox', label: 'Approvals inbox', roles: ['MANAGER'] },
  { to: '/calendar', label: 'Team calendar', roles: ['MANAGER', 'HR'] },
  { to: '/delegations', label: 'Delegation', roles: ['MANAGER'] },
  { to: '/hr/queue', label: 'HR queue', roles: ['HR'] },
  { to: '/hr/escalations', label: 'Escalations', roles: ['HR'] },
  { to: '/hr/analytics', label: 'Analytics', roles: ['HR'] },
  { to: '/hr/holidays', label: 'Holidays', roles: ['HR'] },
  { to: '/apply', label: 'Apply for leave', roles: ['EMPLOYEE', 'MANAGER', 'HR'] },
  { to: '/balances', label: 'My balances', roles: ['EMPLOYEE', 'MANAGER', 'HR'] },
  { to: '/mine', label: 'My requests', roles: ['EMPLOYEE', 'MANAGER', 'HR'] },
]

function Bell({ onOpen }: { onOpen: (id: number) => void }) {
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const { data } = useQuery({ queryKey: ['notifications'], queryFn: api.notifications, refetchInterval: 15_000 })
  const read = useMutation({ mutationFn: (id: number) => api.markRead(id), onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }) })
  const unread = data?.filter((n) => !n.read).length ?? 0

  return (
    <div className="relative">
      <button className="btn-secondary relative !px-3" onClick={() => setOpen((o) => !o)} aria-label={`Notifications, ${unread} unread`}>
        🔔
        {unread > 0 && (
          <span className="absolute -right-1.5 -top-1.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-rose-600 px-1 text-[11px] font-semibold text-white">
            {unread}
          </span>
        )}
      </button>
      {open && (
        <div className="card absolute right-0 z-40 mt-2 max-h-96 w-96 overflow-y-auto">
          {!data?.length && <p className="p-4 text-sm text-slate-500">No notifications yet.</p>}
          {data?.slice(0, 30).map((n) => (
            <button
              key={n.id}
              className={`block w-full border-b border-slate-100 px-4 py-3 text-left text-sm last:border-0 hover:bg-slate-50 ${n.read ? 'text-slate-500' : 'font-medium text-slate-800'}`}
              onClick={() => {
                if (!n.read) read.mutate(n.id)
                if (n.relatedRequestId) {
                  setOpen(false)
                  onOpen(n.relatedRequestId)
                }
              }}
            >
              {n.message}
              <span className="mt-0.5 block text-xs font-normal text-slate-400">{fmtDateTime(n.createdAt)}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

export default function Layout() {
  const { user, logout } = useAuth()
  const qc = useQueryClient()
  const toast = useToast()
  const [openId, setOpenId] = useState<number | null>(null)
  const navigate = useNavigate()
  const location = useLocation()
  const reset = useMutation({
    mutationFn: api.resetSeed,
    onSuccess: () => {
      toast.success('Demo data reloaded')
      qc.invalidateQueries()
    },
    onError: toast.error,
  })
  if (!user) return null

  return (
    <DrawerCtx.Provider value={setOpenId}>
      <div className="flex min-h-screen">
        <aside className="hidden w-60 shrink-0 flex-col border-r border-slate-200 bg-white p-4 md:flex">
          <div className="mb-6 flex items-center gap-2 px-2 text-lg font-bold text-indigo-700">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-600 text-white">L</span> LeaveFlow
          </div>
          <nav className="flex flex-1 flex-col gap-0.5" aria-label="Main">
            {NAV.filter((n) => n.roles.includes(user.role)).map((n) => (
              <NavLink
                key={n.to}
                to={n.to}
                className={({ isActive }) =>
                  `rounded-lg px-3 py-2 text-sm font-medium ${isActive ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:bg-slate-100'}`
                }
              >
                {n.label}
              </NavLink>
            ))}
          </nav>
          {user.role === 'HR' && (
            <button className="btn-secondary mb-3 text-xs" disabled={reset.isPending} onClick={() => reset.mutate()}>
              ↺ Reset demo data
            </button>
          )}
          <div className="border-t border-slate-200 pt-3 text-sm">
            <p className="font-medium text-slate-800">{user.name}</p>
            <p className="text-xs text-slate-500">{user.role.toLowerCase()} · {user.teamName}</p>
            <button className="mt-2 text-xs font-medium text-indigo-600 hover:underline" onClick={logout}>Sign out</button>
          </div>
        </aside>

        <div className="flex min-w-0 flex-1 flex-col">
          <header className="flex items-center justify-between gap-3 border-b border-slate-200 bg-white px-4 py-3 md:justify-end">
            <select
              className="input !w-auto md:hidden"
              aria-label="Navigate"
              value={location.pathname}
              onChange={(e) => navigate(e.target.value)}
            >
              {NAV.filter((n) => n.roles.includes(user.role)).map((n) => (
                <option key={n.to} value={n.to}>{n.label}</option>
              ))}
            </select>
            <Bell onOpen={setOpenId} />
          </header>
          <main className="mx-auto w-full max-w-6xl flex-1 p-4 md:p-8">
            <Outlet />
          </main>
        </div>
      </div>
      {openId !== null && <RequestDrawer id={openId} onClose={() => setOpenId(null)} />}
    </DrawerCtx.Provider>
  )
}
