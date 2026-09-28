import { useQuery } from '@tanstack/react-query'
import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../api'
import { ErrorBox, PageHeader, Spinner, STATUS_LABEL } from '../components/ui'

const COLORS: Record<string, string> = {
  PENDING_MANAGER: '#f59e0b',
  ESCALATED: '#f97316',
  PENDING_HR: '#0ea5e9',
  APPROVED: '#10b981',
  REJECTED: '#f43f5e',
  CANCELLED: '#94a3b8',
}

function Stat({ label, value, hint }: { label: string; value: string | number; hint?: string }) {
  return (
    <div className="card p-5">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 text-3xl font-semibold text-slate-900">{value}</p>
      {hint && <p className="mt-1 text-xs text-slate-400">{hint}</p>}
    </div>
  )
}

export default function Analytics() {
  const { data, isLoading, error } = useQuery({ queryKey: ['analytics'], queryFn: api.analytics })
  if (isLoading) return <Spinner />
  if (error) return <ErrorBox error={error} />
  if (!data) return null

  const statusData = Object.entries(data.byStatus).filter(([, v]) => v > 0).map(([k, v]) => ({ key: k, name: STATUS_LABEL[k as keyof typeof STATUS_LABEL] ?? k, value: v }))
  const escalationRate = data.totalRequests ? Math.round((data.escalatedCount / data.totalRequests) * 100) : 0

  return (
    <>
      <PageHeader title="Analytics" subtitle="Organisation-wide leave activity." />
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Requests" value={data.totalRequests} />
        <Stat label="Conflict-flagged" value={data.flaggedCount} hint="Flagged, never auto-rejected" />
        <Stat label="Escalation rate" value={`${escalationRate}%`} hint={`${data.escalatedCount} timed out to HR`} />
        <Stat label="Avg decision time" value={`${data.avgDecisionHours} h`} hint="Submission to final decision" />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="card p-5">
          <h2 className="mb-4 text-sm font-semibold text-slate-700">Approved leave days by start month</h2>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={data.monthlyTrend}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="month" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="days" name="Days" fill="#4f46e5" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="card p-5">
          <h2 className="mb-4 text-sm font-semibold text-slate-700">Requests by status</h2>
          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie data={statusData} dataKey="value" nameKey="name" innerRadius={55} outerRadius={90} paddingAngle={2} label={({ value }) => value}>
                {statusData.map((s) => <Cell key={s.key} fill={COLORS[s.key] ?? '#94a3b8'} />)}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>

        <div className="card p-5 lg:col-span-2">
          <h2 className="mb-4 text-sm font-semibold text-slate-700">Load by team</h2>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={data.teamLoad}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="teamName" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Bar dataKey="approved" name="Approved" fill="#10b981" radius={[4, 4, 0, 0]} />
              <Bar dataKey="pending" name="Pending" fill="#f59e0b" radius={[4, 4, 0, 0]} />
              <Bar dataKey="flagged" name="Flagged" fill="#f43f5e" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </>
  )
}
