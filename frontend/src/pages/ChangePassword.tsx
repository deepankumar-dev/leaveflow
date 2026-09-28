import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth'
import { useToast } from '../components/Toast'
import { PageHeader } from '../components/ui'

/** Used both as the forced first-sign-in screen (`forced`) and as a normal page. */
export default function ChangePassword({ forced = false }: { forced?: boolean }) {
  const { user, setSession, logout } = useAuth()
  const toast = useToast()
  const nav = useNavigate()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [again, setAgain] = useState('')

  const change = useMutation({
    mutationFn: () => api.changePassword(current, next),
    onSuccess: (res) => {
      setSession(res.token, res.user)
      toast.success('Password changed')
      setCurrent('')
      setNext('')
      setAgain('')
      if (forced) nav('/', { replace: true })
    },
    onError: toast.error,
  })
  const mismatch = again.length > 0 && next !== again
  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!mismatch) change.mutate()
  }

  const form = (
    <form onSubmit={submit} className="card w-full max-w-md space-y-4 p-6">
      {forced && (
        <p className="rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-800">
          Welcome, {user?.name}. Please choose your own password before you continue.
        </p>
      )}
      <div>
        <label className="label" htmlFor="cur">{forced ? 'Temporary password' : 'Current password'}</label>
        <input id="cur" type="password" className="input" required autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} />
      </div>
      <div>
        <label className="label" htmlFor="new">New password</label>
        <input id="new" type="password" className="input" required minLength={10} autoComplete="new-password" value={next} onChange={(e) => setNext(e.target.value)} />
        <p className="mt-1 text-xs text-slate-500">At least 10 characters, with a letter and a digit.</p>
      </div>
      <div>
        <label className="label" htmlFor="again">Repeat new password</label>
        <input id="again" type="password" className="input" required autoComplete="new-password" value={again} onChange={(e) => setAgain(e.target.value)} />
        {mismatch && <p className="mt-1 text-xs text-rose-600">The passwords do not match.</p>}
      </div>
      <div className="flex gap-2">
        <button className="btn-primary" disabled={change.isPending || mismatch}>Change password</button>
        {forced && <button type="button" className="btn-secondary" onClick={logout}>Sign out</button>}
      </div>
    </form>
  )

  if (forced) return <div className="flex min-h-screen items-center justify-center p-4">{form}</div>
  return (
    <>
      <PageHeader title="Change password" />
      {form}
    </>
  )
}
