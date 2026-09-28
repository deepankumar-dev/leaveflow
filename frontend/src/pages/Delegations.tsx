import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api'
import { useAuth } from '../auth'
import { useToast } from '../components/Toast'
import { Empty, ErrorBox, PageHeader, Spinner, fmtRange } from '../components/ui'

export default function Delegations() {
  const { user } = useAuth()
  const qc = useQueryClient()
  const toast = useToast()
  const [delegateId, setDelegateId] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  const list = useQuery({ queryKey: ['delegations'], queryFn: api.delegations })
  const dir = useQuery({ queryKey: ['directory'], queryFn: api.directory })
  const others = dir.data?.managers.filter((m) => m.id !== user?.id) ?? []

  const create = useMutation({
    mutationFn: () => api.delegate(Number(delegateId), from, to),
    onSuccess: (d) => {
      toast.success(`Approvals delegated to ${d.delegateName}`)
      setDelegateId('')
      setFrom('')
      setTo('')
      qc.invalidateQueries({ queryKey: ['delegations'] })
    },
    onError: toast.error,
  })
  const revoke = useMutation({
    mutationFn: (id: number) => api.revokeDelegation(id),
    onSuccess: () => {
      toast.success('Delegation revoked')
      qc.invalidateQueries({ queryKey: ['delegations'] })
    },
    onError: toast.error,
  })
  const submit = (e: FormEvent) => {
    e.preventDefault()
    create.mutate()
  }

  return (
    <>
      <PageHeader title="Delegation" subtitle="While you are away, new requests for you are routed to your delegate. A delegate never decides their own request." />
      <div className="grid gap-6 lg:grid-cols-2">
        <form onSubmit={submit} className="card space-y-4 p-6">
          <h2 className="text-sm font-semibold text-slate-700">Delegate my approvals</h2>
          <div>
            <label className="label" htmlFor="delegate">Delegate to</label>
            <select id="delegate" className="input" required value={delegateId} onChange={(e) => setDelegateId(e.target.value)}>
              <option value="">Choose a manager…</option>
              {others.map((m) => <option key={m.id} value={m.id}>{m.name}{m.teamName ? ` (${m.teamName})` : ''}</option>)}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label" htmlFor="dfrom">From</label>
              <input id="dfrom" type="date" className="input" required value={from} onChange={(e) => setFrom(e.target.value)} />
            </div>
            <div>
              <label className="label" htmlFor="dto">To</label>
              <input id="dto" type="date" className="input" required min={from || undefined} value={to} onChange={(e) => setTo(e.target.value)} />
            </div>
          </div>
          <button className="btn-primary" disabled={create.isPending}>Delegate</button>
        </form>

        <div>
          <h2 className="mb-3 text-sm font-semibold text-slate-700">Active delegations</h2>
          {list.isLoading && <Spinner />}
          {list.error && <ErrorBox error={list.error} />}
          {list.data?.length === 0 && <Empty title="No active delegations" />}
          <div className="space-y-2">
            {list.data?.map((d) => (
              <div key={d.id} className="card flex items-center justify-between px-4 py-3 text-sm">
                <div>
                  <p className="font-medium text-slate-800">→ {d.delegateName}</p>
                  <p className="text-xs text-slate-500">{fmtRange(d.fromDate, d.toDate)}</p>
                </div>
                <button className="btn-secondary !text-xs" disabled={revoke.isPending} onClick={() => revoke.mutate(d.id)}>Revoke</button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </>
  )
}
