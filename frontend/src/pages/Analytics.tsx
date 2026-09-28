import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
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

function ForecastCard() {
  const [teamId, setTeamId] = useState<number | undefined>()
  const dir = useQuery({ queryKey: ['directory'], queryFn: api.directory })
  const { data, error } = useQuery({ queryKey: ['forecast', teamId], queryFn: () => api.forecast(teamId, 30) })
  const rows = data?.days.filter((d) => d.workingDay).map((d) => ({ ...d, label: d.date.slice(5) })) ?? []
  const limit = data ? (data.teamSize * data.thresholdPercent) / 100 : 0
  return (
    <div className="card p-5 lg:col-span-2">
      <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-sm font-semibold text-slate-700">Team capacity forecast: next 30 days</h2>
        <select className="input !w-auto" aria-label="Team" value={teamId ?? ''} onChange={(e) => setTeamId(e.target.value ? Number(e.target.value) : undefined)}>
          <option value="">All teams</option>
          {dir.data?.teams.filter((t) => t.name !== 'HR').map((t) => <option key={t.id} value={t.id}>{t.name} ({t.size})</option>)}
        </select>
      </div>
      <p className="mb-3 text-xs text-slate-500">
        People away per working day: approved plus pending (tentative) leave. Red line = coverage threshold ({data?.thresholdPercent}% of {data?.teamSize} = {limit.toFixed(1)} people).
        Dashed line = average away per day over the last 90 days ({data?.historicalAvgAway}). Plain arithmetic on known leave, not a prediction model.
      </p>
      {error && <ErrorBox error={error} />}
      <ResponsiveContainer width="100%" height={280}>
        <BarChart data={rows}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
          <XAxis dataKey="label" tick={{ fontSize: 11 }} interval={1} />
          <YAxis tick={{ fontSize: 12 }} allowDecimals={false} domain={[0, (max: number) => Math.ceil(Math.max(max, limit)) + 1]} />
          <Tooltip />
          <Legend />
          <Bar dataKey="approvedAway" name="Approved" stackId="a" fill="#4f46e5" />
          <Bar dataKey="pendingAway" name="Pending (tentative)" stackId="a" fill="#fbbf24" />
          <ReferenceLine y={limit} stroke="#e11d48" strokeWidth={2} label={{ value: 'threshold', fill: '#e11d48', fontSize: 11, position: 'insideTopRight' }} />
          <ReferenceLine y={data?.historicalAvgAway ?? 0} stroke="#64748b" strokeDasharray="5 4" />
        </BarChart>
      </ResponsiveContainer>
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

        <ForecastCard />

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
