import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api'
import { useAuth } from '../auth'
import { useOpenRequest } from '../components/Layout'
import { ErrorBox, PageHeader, Spinner } from '../components/ui'

const THRESHOLD = 0.3

const pad = (n: number) => String(n).padStart(2, '0')

export default function TeamCalendar() {
  const { user } = useAuth()
  const open = useOpenRequest()
  const [month, setMonth] = useState(() => new Date().toISOString().slice(0, 7))
  const [teamId, setTeamId] = useState<number | undefined>()

  const dir = useQuery({ queryKey: ['directory'], queryFn: api.directory, enabled: user?.role === 'HR' })
  const cal = useQuery({ queryKey: ['calendar', month, teamId], queryFn: () => api.calendar(month, teamId) })

  const days = useMemo(() => {
    const [y, m] = month.split('-').map(Number)
    const count = new Date(y, m, 0).getDate()
    return Array.from({ length: count }, (_, i) => {
      const d = new Date(y, m - 1, i + 1)
      return { n: i + 1, iso: `${month}-${pad(i + 1)}`, weekend: d.getDay() === 0 || d.getDay() === 6, dow: 'SMTWTFS'[d.getDay()] }
    })
  }, [month])

  const data = cal.data
  const holidays = new Map(data?.holidays.map((h) => [h.date, h.name]))
  const people = useMemo(() => {
    const map = new Map<number, string>()
    data?.entries.forEach((e) => map.set(e.employeeId, e.employeeName))
    return [...map.entries()].sort((a, b) => a[1].localeCompare(b[1]))
  }, [data])

  const awayOn = (iso: string) => new Set(data?.entries.filter((e) => e.fromDate <= iso && e.toDate >= iso).map((e) => e.employeeId)).size

  return (
    <>
      <PageHeader title="Team calendar" subtitle="Approved and pending leave. Red columns are over the coverage threshold (30% away)."
        action={
          <div className="flex gap-2">
            {user?.role === 'HR' && dir.data && (
              <select className="input !w-auto" aria-label="Team" value={teamId ?? ''} onChange={(e) => setTeamId(e.target.value ? Number(e.target.value) : undefined)}>
                <option value="">My team</option>
                {dir.data.teams.map((t) => <option key={t.id} value={t.id}>{t.name} ({t.size})</option>)}
              </select>
            )}
            <input type="month" className="input !w-auto" value={month} onChange={(e) => e.target.value && setMonth(e.target.value)} aria-label="Month" />
          </div>
        } />
      {cal.isLoading && <Spinner />}
      {cal.error && <ErrorBox error={cal.error} />}
      {data && (
        <div className="card overflow-x-auto p-4">
          <p className="mb-3 text-sm text-slate-600"><b>{data.teamName}</b> · {data.teamSize} people</p>
          <table className="border-collapse text-xs">
            <thead>
              <tr>
                <th className="sticky left-0 bg-white pr-3 text-left font-medium text-slate-500">Person</th>
                {days.map((d) => (
                  <th key={d.iso} title={holidays.get(d.iso)} className={`h-10 w-7 min-w-7 text-center font-normal ${holidays.has(d.iso) ? 'bg-amber-100 text-amber-800' : d.weekend ? 'bg-slate-100 text-slate-400' : 'text-slate-500'}`}>
                    <div>{d.dow}</div><div className="font-medium">{d.n}</div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {people.length === 0 && (
                <tr><td colSpan={days.length + 1} className="py-8 text-center text-slate-500">No leave this month.</td></tr>
              )}
              {people.map(([id, name]) => (
                <tr key={id}>
                  <td className="sticky left-0 whitespace-nowrap bg-white py-1 pr-3 text-sm font-medium text-slate-700">{name}</td>
                  {days.map((d) => {
                    const e = data.entries.find((x) => x.employeeId === id && x.fromDate <= d.iso && x.toDate >= d.iso)
                    return (
                      <td key={d.iso} className={`h-7 border border-white ${d.weekend ? 'bg-slate-100' : holidays.has(d.iso) ? 'bg-amber-100' : 'bg-slate-50'}`}>
                        {e && !d.weekend && !holidays.has(d.iso) && (
                          <button onClick={() => open(e.requestId)} aria-label={`${name}, ${e.status}`} title={`${name} · ${e.leaveTypeCode} · ${e.status.replace('_', ' ').toLowerCase()}`}
                            className={`block h-full w-full rounded-sm ${e.status === 'APPROVED' ? 'bg-indigo-500' : 'bg-amber-300'} ${e.flagged ? 'ring-1 ring-rose-500' : ''}`} />
                        )}
                      </td>
                    )
                  })}
                </tr>
              ))}
              <tr>
                <td className="sticky left-0 bg-white pt-2 pr-3 text-sm font-medium text-slate-500">Away</td>
                {days.map((d) => {
                  const away = d.weekend || holidays.has(d.iso) ? 0 : awayOn(d.iso)
                  const over = data.teamSize > 0 && away / data.teamSize > THRESHOLD
                  return (
                    <td key={d.iso} className={`pt-2 text-center font-semibold ${over ? 'bg-rose-100 text-rose-700' : away ? 'text-slate-700' : 'text-slate-300'}`}>
                      {away || '·'}
                    </td>
                  )
                })}
              </tr>
            </tbody>
          </table>
          <div className="mt-4 flex flex-wrap gap-4 text-xs text-slate-500">
            <span><i className="mr-1 inline-block h-3 w-3 rounded-sm bg-indigo-500 align-middle" />Approved</span>
            <span><i className="mr-1 inline-block h-3 w-3 rounded-sm bg-amber-300 align-middle" />Pending (tentative)</span>
            <span><i className="mr-1 inline-block h-3 w-3 rounded-sm bg-amber-100 align-middle ring-1 ring-amber-200" />Holiday</span>
            <span><i className="mr-1 inline-block h-3 w-3 rounded-sm bg-white align-middle ring-1 ring-rose-500" />Conflict-flagged</span>
          </div>
        </div>
      )}
    </>
  )
}
