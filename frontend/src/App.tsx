import type { ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import type { Role } from './api'
import { useAuth } from './auth'
import Layout from './components/Layout'
import { Spinner } from './components/ui'
import Login from './pages/Login'
import Apply from './pages/Apply'
import Balances from './pages/Balances'
import MyRequests from './pages/MyRequests'
import Inbox from './pages/Inbox'
import TeamCalendar from './pages/TeamCalendar'
import Delegations from './pages/Delegations'
import HrQueue from './pages/HrQueue'
import Analytics from './pages/Analytics'
import Holidays from './pages/Holidays'
import People from './pages/People'
import ChangePassword from './pages/ChangePassword'

export const home = (role: Role) => (role === 'MANAGER' ? '/inbox' : role === 'HR' ? '/hr/queue' : '/apply')

/** UI-level guard only; the backend enforces every permission independently. */
function Guard({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (!roles.includes(user.role)) return <Navigate to={home(user.role)} replace />
  return <>{children}</>
}

export default function App() {
  const { user, loading } = useAuth()
  if (loading) return <Spinner />
  // A new or reset account can do nothing else until it has chosen its own password (the server enforces this too).
  if (user?.mustChangePassword) return <ChangePassword forced />

  const all: Role[] = ['EMPLOYEE', 'MANAGER', 'HR']
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to={home(user.role)} replace /> : <Login />} />
      <Route element={user ? <Layout /> : <Navigate to="/login" replace />}>
        <Route path="/apply" element={<Guard roles={all}><Apply /></Guard>} />
        <Route path="/balances" element={<Guard roles={all}><Balances /></Guard>} />
        <Route path="/mine" element={<Guard roles={all}><MyRequests /></Guard>} />
        <Route path="/inbox" element={<Guard roles={['MANAGER']}><Inbox /></Guard>} />
        <Route path="/calendar" element={<Guard roles={['MANAGER', 'HR']}><TeamCalendar /></Guard>} />
        <Route path="/delegations" element={<Guard roles={['MANAGER']}><Delegations /></Guard>} />
        <Route path="/hr/queue" element={<Guard roles={['HR']}><HrQueue mode="queue" /></Guard>} />
        <Route path="/hr/escalations" element={<Guard roles={['HR']}><HrQueue mode="escalations" /></Guard>} />
        <Route path="/hr/analytics" element={<Guard roles={['HR']}><Analytics /></Guard>} />
        <Route path="/password" element={<Guard roles={all}><ChangePassword /></Guard>} />
        <Route path="/hr/people" element={<Guard roles={['HR']}><People /></Guard>} />
        <Route path="/hr/holidays" element={<Guard roles={['HR']}><Holidays /></Guard>} />
      </Route>
      <Route path="*" element={<Navigate to={user ? home(user.role) : '/login'} replace />} />
    </Routes>
  )
}
