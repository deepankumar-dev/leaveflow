import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api'
import { useAuth } from '../auth'
import { useToast } from './Toast'
import { useConfig } from './useConfig'
import { ErrorBox, FlagBadge, Spinner, StatusBadge, fmtDateTime, fmtDays, fmtRange } from './ui'

const ACTION_LABEL: Record<string, string> = {
  SUBMITTED: 'Submitted',
  FLAGGED: 'Conflict flagged',
  DELEGATED: 'Routed to delegate',
  MANAGER_APPROVED: 'Manager approved',
  HR_APPROVED: 'HR approved',
  REJECTED: 'Rejected',
  ESCALATED: 'Escalated to HR',
  CANCELLED: 'Cancelled',
}

/** Side panel for one request: details, approval steps, audit timeline, state diagram and the actions you may take. */
export function RequestDrawer({ id, onClose }: { id: number; onClose: () => void }) {
  const { user } = useAuth()
  const qc = useQueryClient()
  const toast = useToast()
  const { demoMode } = useConfig()
  const [comment, setComment] = useState('')

  const req = useQuery({ queryKey: ['request', id], queryFn: () => api.request(id) })
  const timeline = useQuery({ queryKey: ['timeline', id], queryFn: () => api.timeline(id) })

  const refresh = () => qc.invalidateQueries()
  const done = (msg: string) => () => {
    toast.success(msg)
    setComment('')
    refresh()
  }
  const approve = useMutation({ mutationFn: () => api.approve(id, comment || undefined), onSuccess: done('Approved'), onError: toast.error })
  const reject = useMutation({ mutationFn: () => api.reject(id, comment), onSuccess: done('Rejected'), onError: toast.error })
  const cancel = useMutation({ mutationFn: () => api.cancel(id, comment || undefined), onSuccess: done('Cancelled'), onError: toast.error })
  const timeout = useMutation({ mutationFn: () => api.simulateTimeout(id), onSuccess: done('Manager step timed out: escalated to HR'), onError: toast.error })

  const r = req.data
  const busy = approve.isPending || reject.isPending || cancel.isPending || timeout.isPending
  const canDemoTimeout = demoMode && r?.status === 'PENDING_MANAGER' && (user?.role === 'MANAGER' || user?.role === 'HR')

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-900/30" onClick={onClose}>
      <aside
        className="h-full w-full max-w-xl overflow-y-auto bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label="Leave request details"
      >
        <div className="mb-4 flex items-start justify-between">
          <h2 className="text-lg font-semibold text-slate-900">Request #{id}</h2>
          <button className="btn-secondary !px-2.5 !py-1" onClick={onClose} aria-label="Close">✕</button>
        </div>

        {req.isLoading && <Spinner />}
        {req.error && <ErrorBox error={req.error} />}

        {r && (
          <div className="space-y-6">
            <section className="space-y-2">
              <div className="flex flex-wrap items-center gap-2">
                <StatusBadge status={r.status} />
                {r.flagged && <FlagBadge reason={r.flagReason} />}
              </div>
              <p className="text-base font-medium text-slate-900">{r.employeeName} <span className="font-normal text-slate-500">· {r.teamName}</span></p>
              <p className="text-sm text-slate-600">
                {r.leaveTypeName} · {fmtRange(r.fromDate, r.toDate)} · <b>{fmtDays(r.days)}</b> working day(s)
              </p>
              {r.reason && <p className="text-sm text-slate-500">“{r.reason}”</p>}
              {r.flagged && (
                <div className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-800">
                  <b>Team coverage:</b> {r.flagReason}
                  <div className="mt-0.5 text-xs text-rose-600">A flag is informational; it never blocks a decision.</div>
                </div>
              )}
            </section>

            <section>
              <h3 className="mb-2 text-sm font-semibold text-slate-700">Approval chain</h3>
              <ol className="space-y-2">
                {r.steps.map((s) => (
                  <li key={s.id} className="flex items-start justify-between rounded-lg border border-slate-200 px-3 py-2 text-sm">
                    <div>
                      <span className="font-medium">{s.stage === 'MANAGER' ? 'Manager' : 'HR'}</span>
                      <span className="text-slate-500"> · {s.assigneeName ?? 'unassigned'}</span>
                      {s.delegatedFromName && <span className="ml-1 text-xs text-indigo-600">(delegated from {s.delegatedFromName})</span>}
                      {s.comment && <div className="mt-0.5 text-xs text-slate-500">“{s.comment}”</div>}
                    </div>
                    <span className={`text-xs font-medium ${s.decision === 'PENDING' ? 'text-amber-600' : s.decision === 'APPROVED' ? 'text-emerald-600' : s.decision === 'ESCALATED' ? 'text-orange-600' : 'text-rose-600'}`}>
                      {s.decision.toLowerCase()}
                    </span>
                  </li>
                ))}
              </ol>
            </section>

            <section>
              <h3 className="mb-2 text-sm font-semibold text-slate-700">Audit timeline</h3>
              {timeline.isLoading && <Spinner />}
              <ol className="relative ml-2 space-y-4 border-l border-slate-200 pl-5">
                {timeline.data?.map((e) => (
                  <li key={e.id} className="relative">
                    <span className="absolute -left-[27px] top-1 h-2.5 w-2.5 rounded-full bg-indigo-500 ring-4 ring-white" />
                    <p className="text-sm font-medium text-slate-800">
                      {ACTION_LABEL[e.action] ?? e.action}
                      <span className="font-normal text-slate-500"> · {e.actorName}</span>
                    </p>
                    <p className="text-xs text-slate-400">{fmtDateTime(e.at)}</p>
                    {e.comment && <p className="mt-0.5 text-sm text-slate-600">“{e.comment}”</p>}
                  </li>
                ))}
              </ol>
            </section>

            {(r.canApprove || r.canCancel || canDemoTimeout) && (
              <section className="space-y-3 rounded-xl border border-slate-200 bg-slate-50 p-4">
                <h3 className="text-sm font-semibold text-slate-700">Actions</h3>
                {(r.canApprove || r.canCancel) && (
                  <div>
                    <label className="label" htmlFor="decision-comment">Comment {r.canApprove && '(required to reject)'}</label>
                    <textarea id="decision-comment" className="input" rows={2} value={comment} onChange={(e) => setComment(e.target.value)} />
                  </div>
                )}
                <div className="flex flex-wrap gap-2">
                  {r.canApprove && (
                    <>
                      <button className="btn-primary" disabled={busy} onClick={() => approve.mutate()}>Approve</button>
                      <button className="btn-danger" disabled={busy || !comment.trim()} onClick={() => reject.mutate()}
                        title={!comment.trim() ? 'Add a comment to reject' : undefined}>Reject</button>
                    </>
                  )}
                  {r.canCancel && (
                    <button className="btn-secondary" disabled={busy} onClick={() => cancel.mutate()}>
                      {r.status === 'APPROVED' ? 'Cancel approved leave' : 'Withdraw request'}
                    </button>
                  )}
                  {canDemoTimeout && (
                    <button className="btn-secondary border-amber-300 text-amber-700" disabled={busy} onClick={() => timeout.mutate()}
                      title="Demo: force the manager step to time out now">⏱ Simulate timeout</button>
                  )}
                </div>
              </section>
            )}
          </div>
        )}
      </aside>
    </div>
  )
}
