import type { LeaveRequest } from '../api'
import { useOpenRequest } from './Layout'
import { FlagBadge, StatusBadge, fmtDays, fmtRange } from './ui'

/** Clickable list of requests; a click opens the side panel with the timeline and workflow. */
export function RequestTable({
  rows,
  showEmployee = true,
  extra,
}: {
  rows: LeaveRequest[]
  showEmployee?: boolean
  extra?: (r: LeaveRequest) => React.ReactNode
}) {
  const open = useOpenRequest()
  return (
    <div className="card overflow-x-auto">
      <table className="w-full min-w-[640px] text-left text-sm">
        <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
          <tr>
            <th className="px-4 py-3">#</th>
            {showEmployee && <th className="px-4 py-3">Employee</th>}
            <th className="px-4 py-3">Type</th>
            <th className="px-4 py-3">Dates</th>
            <th className="px-4 py-3 text-right">Days</th>
            <th className="px-4 py-3">Status</th>
            {extra && <th className="px-4 py-3">Deadline</th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id} tabIndex={0} className="cursor-pointer border-b border-slate-100 last:border-0 hover:bg-indigo-50/40 focus:bg-indigo-50/40"
              onClick={() => open(r.id)} onKeyDown={(e) => e.key === 'Enter' && open(r.id)}>
              <td className="px-4 py-3 text-slate-400">{r.id}</td>
              {showEmployee && (
                <td className="px-4 py-3 font-medium text-slate-800">
                  {r.employeeName}<div className="text-xs font-normal text-slate-400">{r.teamName}</div>
                </td>
              )}
              <td className="px-4 py-3">{r.leaveTypeName}</td>
              <td className="px-4 py-3 whitespace-nowrap">{fmtRange(r.fromDate, r.toDate)}</td>
              <td className="px-4 py-3 text-right">{fmtDays(r.days)}</td>
              <td className="px-4 py-3">
                <div className="flex flex-wrap items-center gap-1.5">
                  <StatusBadge status={r.status} />
                  {r.flagged && <FlagBadge reason={r.flagReason} />}
                </div>
              </td>
              {extra && <td className="px-4 py-3">{extra(r)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
