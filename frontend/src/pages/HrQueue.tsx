import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api'
import { RequestTable } from '../components/RequestTable'
import { Empty, ErrorBox, PageHeader, Spinner } from '../components/ui'

const FILTERS = [
  { key: 'ACTION', label: 'Needs HR action' },
  { key: '', label: 'All requests' },
  { key: 'APPROVED', label: 'Approved' },
  { key: 'REJECTED', label: 'Rejected' },
  { key: 'CANCELLED', label: 'Cancelled' },
]

export default function HrQueue({ mode }: { mode: 'queue' | 'escalations' }) {
  const [filter, setFilter] = useState('ACTION')
  const [q, setQ] = useState('')

  const escalations = mode === 'escalations'
  const { data, isLoading, error } = useQuery({
    queryKey: ['hr-requests', mode, filter, q],
    queryFn: () => api.hrRequests({ status: escalations ? 'ESCALATED' : filter === 'ACTION' ? undefined : filter || undefined, q: q || undefined }),
    refetchInterval: 10_000,
  })

  // "Needs HR action" = waiting on HR, either directly or because the manager did not respond.
  const rows = (data?.content ?? []).filter((r) => escalations || filter !== 'ACTION' || r.status === 'PENDING_HR' || r.status === 'ESCALATED')

  return (
    <>
      <PageHeader
        title={escalations ? 'Escalations' : 'HR queue'}
        subtitle={escalations ? 'Requests whose manager step timed out. HR decides; the system never approves or rejects.' : 'Every request in the organisation.'}
        action={<input className="input !w-56" placeholder="Search name or reason…" aria-label="Search" value={q} onChange={(e) => setQ(e.target.value)} />}
      />
      {!escalations && (
        <div className="mb-4 flex flex-wrap gap-2" role="tablist">
          {FILTERS.map((f) => (
            <button key={f.key} role="tab" aria-selected={filter === f.key} onClick={() => setFilter(f.key)}
              className={`rounded-full px-3.5 py-1.5 text-sm font-medium ${filter === f.key ? 'bg-indigo-600 text-white' : 'border border-slate-300 bg-white text-slate-600 hover:bg-slate-50'}`}>
              {f.label}
            </button>
          ))}
        </div>
      )}
      {isLoading && <Spinner />}
      {error && <ErrorBox error={error} />}
      {data && rows.length === 0 && <Empty title={escalations ? 'No escalations' : 'Nothing here'} hint={escalations ? 'When a manager does not respond in time, the request lands here.' : 'Try another filter.'} />}
      {rows.length > 0 && <RequestTable rows={rows} />}
    </>
  )
}
