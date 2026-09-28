import type { Status } from '../api'
import { STATUS_LABEL } from './ui'

// Layout of the workflow from docs/RULES.md section 11. The active state is highlighted.
const W = 124
const H = 38
const NODES: Record<Status, { x: number; y: number }> = {
  PENDING_MANAGER: { x: 10, y: 50 },
  PENDING_HR: { x: 210, y: 50 },
  APPROVED: { x: 410, y: 50 },
  ESCALATED: { x: 110, y: 140 },
  REJECTED: { x: 260, y: 215 },
  CANCELLED: { x: 410, y: 215 },
}

const right = (s: Status) => ({ x: NODES[s].x + W, y: NODES[s].y + H / 2 })
const left = (s: Status) => ({ x: NODES[s].x, y: NODES[s].y + H / 2 })
const bottom = (s: Status) => ({ x: NODES[s].x + W / 2, y: NODES[s].y + H })
const top = (s: Status) => ({ x: NODES[s].x + W / 2, y: NODES[s].y })

export function StateDiagram({ current }: { current: Status }) {
  const isDone = current === 'APPROVED' || current === 'REJECTED' || current === 'CANCELLED'
  return (
    <div>
      <svg viewBox="0 0 550 270" className="w-full" role="img" aria-label={`Request state: ${STATUS_LABEL[current]}`}>
        <defs>
          <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
            <path d="M0 0L10 5L0 10z" fill="#94a3b8" />
          </marker>
        </defs>

        {/* manager approves → HR */}
        <line x1={right('PENDING_MANAGER').x} y1={right('PENDING_MANAGER').y} x2={left('PENDING_HR').x - 2} y2={left('PENDING_HR').y}
          stroke="#94a3b8" strokeWidth="1.5" markerEnd="url(#arrow)" />
        <text x={172} y={62} textAnchor="middle" fontSize="9" fill="#64748b">approve</text>
        {/* HR approves → approved */}
        <line x1={right('PENDING_HR').x} y1={right('PENDING_HR').y} x2={left('APPROVED').x - 2} y2={left('APPROVED').y}
          stroke="#94a3b8" strokeWidth="1.5" markerEnd="url(#arrow)" />
        <text x={372} y={62} textAnchor="middle" fontSize="9" fill="#64748b">HR approves</text>
        {/* types with no HR step: manager approval ends the chain */}
        <path d={`M${top('PENDING_MANAGER').x} ${top('PENDING_MANAGER').y} C ${top('PENDING_MANAGER').x} 5, ${top('APPROVED').x} 5, ${top('APPROVED').x} ${top('APPROVED').y - 2}`}
          fill="none" stroke="#94a3b8" strokeWidth="1.5" markerEnd="url(#arrow)" />
        <text x={275} y={14} textAnchor="middle" fontSize="9" fill="#64748b">type without HR step</text>
        {/* timeout → escalated → HR decides */}
        <line x1={bottom('PENDING_MANAGER').x + 20} y1={bottom('PENDING_MANAGER').y} x2={top('ESCALATED').x - 20} y2={top('ESCALATED').y - 2}
          stroke="#f59e0b" strokeWidth="1.5" strokeDasharray="4 3" markerEnd="url(#arrow)" />
        <text x={62} y={120} fontSize="9" fill="#b45309">timeout (system)</text>
        <path d={`M${right('ESCALATED').x} ${right('ESCALATED').y} C 380 ${right('ESCALATED').y}, 470 130, ${bottom('APPROVED').x} ${bottom('APPROVED').y + 2}`}
          fill="none" stroke="#94a3b8" strokeWidth="1.5" markerEnd="url(#arrow)" />
        <text x={350} y={150} fontSize="9" fill="#64748b">HR approves</text>
        {/* reject / cancel */}
        <line x1={bottom('PENDING_HR').x} y1={bottom('PENDING_HR').y} x2={top('REJECTED').x} y2={top('REJECTED').y - 2}
          stroke="#fda4af" strokeWidth="1.5" strokeDasharray="4 3" markerEnd="url(#arrow)" />
        <text x={283} y={170} fontSize="9" fill="#e11d48">reject (comment)</text>
        <line x1={bottom('APPROVED').x - 30} y1={bottom('APPROVED').y} x2={top('CANCELLED').x - 30} y2={top('CANCELLED').y - 2}
          stroke="#cbd5e1" strokeWidth="1.5" strokeDasharray="4 3" markerEnd="url(#arrow)" />
        <text x={396} y={195} fontSize="9" fill="#64748b" textAnchor="end">cancel</text>

        {(Object.keys(NODES) as Status[]).map((s) => {
          const active = s === current
          const terminal = s === 'APPROVED' || s === 'REJECTED' || s === 'CANCELLED'
          return (
            <g key={s}>
              <rect x={NODES[s].x} y={NODES[s].y} width={W} height={H} rx={terminal ? 19 : 8}
                fill={active ? '#4f46e5' : '#ffffff'} stroke={active ? '#4338ca' : '#cbd5e1'} strokeWidth={active ? 2 : 1.2} />
              <text x={NODES[s].x + W / 2} y={NODES[s].y + H / 2 + 4} textAnchor="middle" fontSize="11"
                fontWeight={active ? 700 : 500} fill={active ? '#ffffff' : '#334155'}>
                {STATUS_LABEL[s]}
              </text>
            </g>
          )
        })}
      </svg>
      <p className="mt-1 text-xs text-slate-500">
        {isDone ? 'This request has reached a final state.' : 'Highlighted: where this request is now. Nobody decides their own request; the system never approves or rejects.'}
      </p>
    </div>
  )
}
