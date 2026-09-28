import { useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, type PersonInput, type Role, type User } from '../api'
import { useAuth } from '../auth'
import { useToast } from '../components/Toast'
import { ErrorBox, PageHeader, Spinner, fmtDate } from '../components/ui'

const EMPTY: PersonInput = { name: '', email: '', role: 'EMPLOYEE', teamId: null, managerId: null, joinDate: '', password: '', active: true }

export default function People() {
  const { user: me } = useAuth()
  const qc = useQueryClient()
  const toast = useToast()
  const [editing, setEditing] = useState<User | 'new' | null>(null)
  const [form, setForm] = useState<PersonInput>(EMPTY)
  const [showInactive, setShowInactive] = useState(false)
  const [teamName, setTeamName] = useState('')

  const people = useQuery({ queryKey: ['people'], queryFn: api.people })
  const dir = useQuery({ queryKey: ['directory'], queryFn: api.directory })
  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['people'] })
    qc.invalidateQueries({ queryKey: ['directory'] })
  }

  const save = useMutation({
    mutationFn: () => (editing === 'new' ? api.createPerson(form) : api.updatePerson((editing as User).id, form)),
    onSuccess: () => {
      toast.success(editing === 'new' ? 'Person added: they must change the temporary password at first sign-in' : 'Saved')
      setEditing(null)
      refresh()
    },
    onError: toast.error,
  })
  const reset = useMutation({
    mutationFn: ({ id, pw }: { id: number; pw: string }) => api.resetPassword(id, pw),
    onSuccess: () => toast.success('Temporary password set: they must change it at next sign-in'),
    onError: toast.error,
  })
  const addTeam = useMutation({
    mutationFn: () => api.createTeam(teamName),
    onSuccess: () => {
      toast.success('Team added')
      setTeamName('')
      refresh()
    },
    onError: toast.error,
  })

  const managers = useMemo(() => (people.data ?? []).filter((p) => p.role === 'MANAGER' && p.active), [people.data])
  const managersForTeam = managers.filter((m) => m.teamId === form.teamId)

  const startNew = () => {
    setForm(EMPTY)
    setEditing('new')
  }
  const startEdit = (u: User) => {
    setForm({ name: u.name, email: u.email, role: u.role, teamId: u.teamId, managerId: u.managerId, joinDate: u.joinDate, active: u.active })
    setEditing(u)
  }
  const submit = (e: FormEvent) => {
    e.preventDefault()
    save.mutate()
  }
  const doReset = (u: User) => {
    const pw = window.prompt(`New temporary password for ${u.name} (10+ characters, a letter and a digit):`)
    if (pw) reset.mutate({ id: u.id, pw })
  }

  const rows = (people.data ?? []).filter((p) => showInactive || p.active)
  const set = <K extends keyof PersonInput>(k: K, v: PersonInput[K]) => setForm((f) => ({ ...f, [k]: v }))

  return (
    <>
      <PageHeader title="People & teams" subtitle="Add people, assign teams and managers, deactivate leavers. People are never deleted, so their leave history stays intact."
        action={<button className="btn-primary" onClick={startNew}>Add person</button>} />

      {editing && (
        <form onSubmit={submit} className="card mb-6 space-y-4 p-6">
          <h2 className="text-sm font-semibold text-slate-700">{editing === 'new' ? 'New person' : `Edit ${editing.name}`}</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="label" htmlFor="pname">Name</label>
              <input id="pname" className="input" required maxLength={100} value={form.name} onChange={(e) => set('name', e.target.value)} />
            </div>
            <div>
              <label className="label" htmlFor="pemail">Email</label>
              <input id="pemail" type="email" className="input" required value={form.email} onChange={(e) => set('email', e.target.value)} />
            </div>
            <div>
              <label className="label" htmlFor="prole">Role</label>
              <select id="prole" className="input" value={form.role} disabled={editing !== 'new' && editing.id === me?.id}
                onChange={(e) => setForm((f) => ({ ...f, role: e.target.value as Role, managerId: null }))}>
                <option value="EMPLOYEE">Employee</option>
                <option value="MANAGER">Manager</option>
                <option value="HR">HR</option>
              </select>
            </div>
            <div>
              <label className="label" htmlFor="pteam">Team</label>
              <select id="pteam" className="input" required={form.role !== 'HR'} value={form.teamId ?? ''}
                onChange={(e) => setForm((f) => ({ ...f, teamId: e.target.value ? Number(e.target.value) : null, managerId: null }))}>
                <option value="">{form.role === 'HR' ? 'No team' : 'Choose a team…'}</option>
                {dir.data?.teams.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
              </select>
            </div>
            {form.role === 'EMPLOYEE' && (
              <div>
                <label className="label" htmlFor="pmgr">Manager (same team)</label>
                <select id="pmgr" className="input" required value={form.managerId ?? ''} onChange={(e) => set('managerId', e.target.value ? Number(e.target.value) : null)}>
                  <option value="">Choose a manager…</option>
                  {managersForTeam.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
                </select>
                {form.teamId && managersForTeam.length === 0 && <p className="mt-1 text-xs text-amber-700">This team has no manager yet: add one first.</p>}
              </div>
            )}
            <div>
              <label className="label" htmlFor="pjoin">Join date</label>
              <input id="pjoin" type="date" className="input" required value={form.joinDate} onChange={(e) => set('joinDate', e.target.value)} />
              <p className="mt-1 text-xs text-slate-500">Used to pro-rate annual leave in the joining year.</p>
            </div>
            {editing === 'new' && (
              <div>
                <label className="label" htmlFor="ppw">Temporary password</label>
                <input id="ppw" type="text" className="input" required minLength={10} autoComplete="off" value={form.password} onChange={(e) => set('password', e.target.value)} />
                <p className="mt-1 text-xs text-slate-500">10+ characters with a letter and a digit. Share it securely; they must change it at first sign-in.</p>
              </div>
            )}
            {editing !== 'new' && editing.id !== me?.id && (
              <label className="flex items-center gap-2 self-end pb-2 text-sm">
                <input type="checkbox" checked={form.active ?? true} onChange={(e) => set('active', e.target.checked)} /> Active (can sign in)
              </label>
            )}
          </div>
          <div className="flex gap-2">
            <button className="btn-primary" disabled={save.isPending}>{save.isPending ? 'Saving…' : 'Save'}</button>
            <button type="button" className="btn-secondary" onClick={() => setEditing(null)}>Cancel</button>
          </div>
        </form>
      )}

      <div className="mb-3 flex items-center justify-between">
        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input type="checkbox" checked={showInactive} onChange={(e) => setShowInactive(e.target.checked)} /> Show deactivated
        </label>
        <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); if (teamName.trim()) addTeam.mutate() }}>
          <input className="input !w-44" placeholder="New team name" aria-label="New team name" value={teamName} onChange={(e) => setTeamName(e.target.value)} />
          <button className="btn-secondary" disabled={addTeam.isPending || !teamName.trim()}>Add team</button>
        </form>
      </div>

      {people.isLoading && <Spinner />}
      {people.error && <ErrorBox error={people.error} />}
      {people.data && (
        <div className="card overflow-x-auto">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr><th className="px-4 py-3">Name</th><th className="px-4 py-3">Role</th><th className="px-4 py-3">Team</th><th className="px-4 py-3">Manager</th><th className="px-4 py-3">Joined</th><th className="px-4 py-3" /></tr>
            </thead>
            <tbody>
              {rows.map((u) => (
                <tr key={u.id} className={`border-b border-slate-100 last:border-0 ${u.active ? '' : 'text-slate-400'}`}>
                  <td className="px-4 py-3 font-medium">
                    {u.name}{!u.active && <span className="ml-2 rounded bg-slate-100 px-1.5 py-0.5 text-xs">deactivated</span>}
                    {u.mustChangePassword && <span className="ml-2 rounded bg-amber-50 px-1.5 py-0.5 text-xs text-amber-700">password pending</span>}
                    <div className="text-xs font-normal text-slate-400">{u.email}</div>
                  </td>
                  <td className="px-4 py-3">{u.role.toLowerCase()}</td>
                  <td className="px-4 py-3">{u.teamName ?? '–'}</td>
                  <td className="px-4 py-3">{u.managerName ?? '–'}</td>
                  <td className="px-4 py-3 whitespace-nowrap">{fmtDate(u.joinDate)}</td>
                  <td className="px-4 py-3 text-right whitespace-nowrap">
                    <button className="btn-secondary !px-2.5 !py-1 !text-xs" onClick={() => startEdit(u)}>Edit</button>{' '}
                    <button className="btn-secondary !px-2.5 !py-1 !text-xs" onClick={() => doReset(u)}>Reset password</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
