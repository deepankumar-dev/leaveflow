import { useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useToast } from '../components/Toast'
import { PageHeader, fmtDays, fmtRange } from '../components/ui'

const addDays = (iso: string, n: number) => {
  const d = new Date(iso + 'T00:00:00Z')
  d.setUTCDate(d.getUTCDate() + n)
  return d.toISOString().slice(0, 10)
}
const spanDays = (from: string, to: string) => Math.round((Date.parse(to) - Date.parse(from)) / 86_400_000)

export default function Apply() {
  const qc = useQueryClient()
  const toast = useToast()
  const nav = useNavigate()
  const [type, setType] = useState('ANNUAL')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [reason, setReason] = useState('')

  const balances = useQuery({ queryKey: ['balances'], queryFn: api.balances })
  const ready = !!from && !!to && from <= to
  const preview = useQuery({
    queryKey: ['preview', type, from, to],
    queryFn: () => api.preview(type, from, to),
    enabled: ready,
  })

  // "Try these dates instead": probe same-length windows around the chosen dates and offer the ones with no conflict.
  const p = preview.data
  const suggestions = useQuery({
    queryKey: ['suggest', type, from, to],
    enabled: ready && !!p?.conflictFlagged,
    queryFn: async () => {
      const len = spanDays(from, to)
      const shifts = [-14, -7, -3, 3, 7, 14, 21]
      const results = await Promise.all(
        shifts.map(async (s) => {
          const f = addDays(from, s)
          const t = addDays(f, len)
          try {
            return { from: f, to: t, res: await api.preview(type, f, t) }
          } catch {
            return null
          }
        }),
      )
      return results
        .filter((r): r is NonNullable<typeof r> => !!r && r.res.valid && !r.res.conflictFlagged)
        .sort((a, b) => Math.abs(spanDays(from, a.from)) - Math.abs(spanDays(from, b.from)))
        .slice(0, 3)
    },
  })

  const submit = useMutation({
    mutationFn: () => api.apply(type, from, to, reason),
    onSuccess: (r) => {
      toast.success(r.flagged ? 'Submitted. Your manager will see the team-coverage flag.' : 'Leave request submitted')
      qc.invalidateQueries()
      nav('/mine')
    },
    onError: toast.error,
  })

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    submit.mutate()
  }

  const types = useMemo(() => balances.data ?? [], [balances.data])

  return (
    <>
      <PageHeader title="Apply for leave" subtitle="See working days, balance and team impact before you submit." />
      <div className="grid gap-6 lg:grid-cols-5">
        <form onSubmit={onSubmit} className="card space-y-4 p-6 lg:col-span-3">
          <div>
            <label className="label" htmlFor="type">Leave type</label>
            <select id="type" className="input" value={type} onChange={(e) => setType(e.target.value)}>
              {types.length === 0 && <option value="ANNUAL">Annual Leave</option>}
              {types.map((b) => (
                <option key={b.leaveTypeCode} value={b.leaveTypeCode}>
                  {b.leaveTypeName}{b.tracked ? ` (${fmtDays(b.available)} available)` : ' (no limit)'}
                </option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label" htmlFor="from">From</label>
              <input id="from" type="date" className="input" required value={from}
                onChange={(e) => { setFrom(e.target.value); if (!to || to < e.target.value) setTo(e.target.value) }} />
            </div>
            <div>
              <label className="label" htmlFor="to">To</label>
              <input id="to" type="date" className="input" required min={from || undefined} value={to} onChange={(e) => setTo(e.target.value)} />
            </div>
          </div>
          <div>
            <label className="label" htmlFor="reason">Reason (optional)</label>
            <textarea id="reason" className="input" rows={3} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} />
          </div>
          <button className="btn-primary" disabled={!ready || submit.isPending || (p ? !p.valid : false)}>
            {submit.isPending ? 'Submitting…' : 'Submit request'}
          </button>
        </form>

        <div className="space-y-4 lg:col-span-2">
          <div className="card p-5" aria-live="polite">
            <h2 className="mb-3 text-sm font-semibold text-slate-700">Live check</h2>
            {!ready && <p className="text-sm text-slate-500">Choose your dates to see the impact.</p>}
            {ready && preview.isLoading && <p className="text-sm text-slate-500">Checking…</p>}
            {p && (
              <div className="space-y-3 text-sm">
                <div className="flex items-baseline justify-between">
                  <span className="text-slate-500">Working days</span>
                  <span className="text-2xl font-semibold text-slate-900">{fmtDays(p.workingDays)}</span>
                </div>
                {p.balanceAvailable != null && (
                  <div className="flex justify-between">
                    <span className="text-slate-500">Balance after</span>
                    <span className={`font-medium ${(p.balanceAfter ?? 0) < 0 ? 'text-rose-600' : 'text-slate-800'}`}>
                      {fmtDays(p.balanceAvailable)} → {fmtDays(p.balanceAfter)}
                    </span>
                  </div>
                )}
                {p.balanceExplanation && <p className="text-xs text-slate-400">{p.balanceExplanation}</p>}
                {p.errors.map((e) => (
                  <div key={e} className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-rose-700">{e}</div>
                ))}
                {p.conflictFlagged && (
                  <div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-amber-800">
                    <b>⚑ Team coverage conflict.</b> {p.conflictReason}
                    <div className="mt-1 text-xs">You can still submit; your manager will see this flag.</div>
                  </div>
                )}
                {!p.conflictFlagged && p.valid && (
                  <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-emerald-800">No team conflicts on these dates.</div>
                )}
              </div>
            )}
          </div>

          {p?.conflictFlagged && (
            <div className="card p-5">
              <h2 className="mb-3 text-sm font-semibold text-slate-700">Try these dates instead</h2>
              {suggestions.isLoading && <p className="text-sm text-slate-500">Looking for quieter dates…</p>}
              {suggestions.data?.length === 0 && <p className="text-sm text-slate-500">No conflict-free window found nearby.</p>}
              <div className="flex flex-wrap gap-2">
                {suggestions.data?.map((s) => (
                  <button key={s.from} type="button" className="btn-secondary !text-xs" onClick={() => { setFrom(s.from); setTo(s.to) }}>
                    {fmtRange(s.from, s.to)} · {fmtDays(s.res.workingDays)}d
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  )
}
