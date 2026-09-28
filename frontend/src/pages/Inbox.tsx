import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, type LeaveRequest } from '../api'
import { RequestTable } from '../components/RequestTable'
import { Empty, ErrorBox, PageHeader, Spinner } from '../components/ui'

const dueOf = (r: LeaveRequest) => r.steps.find((s) => s.stage === 'MANAGER' && s.decision === 'PENDING')?.dueAt ?? null

/** Live countdown to the manager step's escalation deadline. Far-off deadlines (seed data) read as "no deadline". */
export function Countdown({ due }: { due: string | null }) {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(t)
  }, [])
  if (!due) return <span className="text-slate-400">–</span>
  // The backend clock may run ahead of the browser's in demo mode, so this is an estimate.
  const ms = Date.parse(due) - now
  if (ms > 30 * 86_400_000) return <span className="text-slate-400">no deadline</span>
  if (ms <= 0) return <span className="font-medium text-rose-600">due: escalates on next check</span>
  const s = Math.floor(ms / 1000)
  const label = s >= 3600 ? `${Math.floor(s / 3600)}h ${Math.floor((s % 3600) / 60)}m` : `${Math.floor(s / 60)}m ${String(s % 60).padStart(2, '0')}s`
  return <span className={`font-mono text-xs ${s < 60 ? 'font-semibold text-rose-600' : 'text-amber-700'}`}>{label} left</span>
}

export default function Inbox() {
  const { data, isLoading, error } = useQuery({ queryKey: ['manager-requests'], queryFn: () => api.managerRequests(), refetchInterval: 10_000 })

  const waiting = (data?.content ?? [])
    .filter((r) => r.canApprove)
    .sort((a, b) => (Date.parse(dueOf(a) ?? '9999-01-01') - Date.parse(dueOf(b) ?? '9999-01-01')) || a.id - b.id)
  const rest = (data?.content ?? []).filter((r) => !r.canApprove)

  return (
    <>
      <PageHeader title="Approvals inbox" subtitle="Sorted by time left before the request escalates to HR." />
      {isLoading && <Spinner />}
      {error && <ErrorBox error={error} />}
      {data && (
        <div className="space-y-8">
          <section>
            <h2 className="mb-3 text-sm font-semibold text-slate-700">Waiting for your decision ({waiting.length})</h2>
            {waiting.length === 0 ? (
              <Empty title="You're all caught up" hint="Requests that need your approval appear here." />
            ) : (
              <RequestTable rows={waiting} extra={(r) => (r.status === 'PENDING_MANAGER' ? <Countdown due={dueOf(r)} /> : null)} />
            )}
          </section>
          <section>
            <h2 className="mb-3 text-sm font-semibold text-slate-700">Other requests in your scope ({rest.length})</h2>
            {rest.length === 0 ? <Empty title="Nothing else" /> : <RequestTable rows={rest} />}
          </section>
        </div>
      )}
    </>
  )
}
