import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'
import { useToast } from '../components/Toast'
import { useConfig } from '../components/useConfig'
import { home } from '../App'

const DEMO = [
  { email: 'asha@leave.demo', label: 'Employee', who: 'Asha', hint: 'Engineering' },
  { email: 'ravi@leave.demo', label: 'New joiner', who: 'Ravi', hint: 'Pro-rated balance' },
  { email: 'priya@leave.demo', label: 'Manager', who: 'Priya', hint: 'Engineering (+ delegate)' },
  { email: 'meena@leave.demo', label: 'HR', who: 'Meena', hint: 'Final approvals' },
]
const DEMO_PASSWORD = 'Password@123'

export default function Login() {
  const { login } = useAuth()
  const { demoMode } = useConfig()
  const toast = useToast()
  const nav = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)

  const go = async (e: string, p: string) => {
    setBusy(true)
    try {
      const u = await login(e, p)
      nav(home(u.role), { replace: true })
    } catch (err) {
      toast.error(err)
    } finally {
      setBusy(false)
    }
  }
  const submit = (ev: FormEvent) => {
    ev.preventDefault()
    void go(email, password)
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-600 text-xl font-bold text-white">L</div>
          <h1 className="text-2xl font-semibold text-slate-900">LeaveFlow</h1>
          <p className="mt-1 text-sm text-slate-500">Leave management with a clear approval chain</p>
        </div>

        <div className="card p-6">
          {demoMode && (
            <>
              <p className="mb-3 text-xs font-medium uppercase tracking-wide text-slate-500">Demo: sign in with one click</p>
              <div className="grid grid-cols-2 gap-2">
                {DEMO.map((d) => (
                  <button key={d.email} className="btn-secondary !flex-col !items-start !gap-0 !py-2.5 text-left" disabled={busy} onClick={() => go(d.email, DEMO_PASSWORD)}>
                    <span className="text-sm font-semibold">{d.label} ({d.who})</span>
                    <span className="text-xs font-normal text-slate-500">{d.hint}</span>
                  </button>
                ))}
              </div>

              <div className="my-5 flex items-center gap-3 text-xs text-slate-400">
                <span className="h-px flex-1 bg-slate-200" /> or <span className="h-px flex-1 bg-slate-200" />
              </div>
            </>
          )}

          <form onSubmit={submit} className="space-y-3">
            <div>
              <label className="label" htmlFor="email">Email</label>
              <input id="email" className="input" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" />
            </div>
            <div>
              <label className="label" htmlFor="password">Password</label>
              <input id="password" className="input" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
            </div>
            <button className="btn-primary w-full" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
          </form>
        </div>
      </div>
    </div>
  )
}
