import { useQuery } from '@tanstack/react-query'
import { api } from '../api'
import { ErrorBox, PageHeader, Spinner, fmtDays } from '../components/ui'

export default function Balances() {
  const { data, isLoading, error } = useQuery({ queryKey: ['balances'], queryFn: api.balances })
  return (
    <>
      <PageHeader title="My balances" subtitle={data ? `Leave year ${data[0]?.year}` : undefined} />
      {isLoading && <Spinner />}
      {error && <ErrorBox error={error} />}
      <div className="grid gap-4 sm:grid-cols-2">
        {data?.map((b) => {
          const total = b.entitled ?? 0
          const used = total ? ((b.used ?? 0) / total) * 100 : 0
          const pending = total ? ((b.pending ?? 0) / total) * 100 : 0
          return (
            <div key={b.leaveTypeCode} className="card p-5">
              <div className="flex items-baseline justify-between">
                <h2 className="font-semibold text-slate-900">{b.leaveTypeName}</h2>
                {b.tracked ? (
                  <span className="text-2xl font-semibold text-indigo-700">{fmtDays(b.available)} <span className="text-sm font-normal text-slate-500">left</span></span>
                ) : (
                  <span className="text-sm text-slate-500">No limit</span>
                )}
              </div>
              {b.tracked && (
                <>
                  <div className="mt-4 flex h-2.5 overflow-hidden rounded-full bg-slate-100" role="img"
                    aria-label={`${fmtDays(b.used)} used, ${fmtDays(b.pending)} pending of ${fmtDays(b.entitled)}`}>
                    <div className="bg-indigo-600" style={{ width: `${used}%` }} />
                    <div className="bg-amber-400" style={{ width: `${pending}%` }} />
                  </div>
                  <dl className="mt-3 grid grid-cols-3 text-center text-xs text-slate-500">
                    <div><dt>Entitled</dt><dd className="text-base font-semibold text-slate-800">{fmtDays(b.entitled)}</dd></div>
                    <div><dt>Used</dt><dd className="text-base font-semibold text-slate-800">{fmtDays(b.used)}</dd></div>
                    <div><dt>Pending</dt><dd className="text-base font-semibold text-amber-600">{fmtDays(b.pending)}</dd></div>
                  </dl>
                </>
              )}
              <p className="mt-4 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-600" title="How this number is calculated">
                <b>How it's calculated:</b> {b.explanation}
              </p>
            </div>
          )
        })}
      </div>
    </>
  )
}
