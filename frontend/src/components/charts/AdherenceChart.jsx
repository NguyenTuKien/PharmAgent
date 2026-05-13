import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

const demoData = [
  { day: 'T2', adherence: 82 },
  { day: 'T3', adherence: 88 },
  { day: 'T4', adherence: 76 },
  { day: 'T5', adherence: 91 },
  { day: 'T6', adherence: 86 },
  { day: 'T7', adherence: 94 },
  { day: 'CN', adherence: 90 },
]

export function AdherenceChart({ data = demoData }) {
  return (
    <div className="chart-card">
      <div>
        <p className="eyebrow">Tuan hien tai</p>
        <h3>Ti le uong thuoc dung lich</h3>
      </div>
      <div className="chart-frame">
        <ResponsiveContainer height={220} width="100%">
          <AreaChart data={data} margin={{ top: 12, right: 12, left: -18, bottom: 0 }}>
            <defs>
              <linearGradient id="adherenceGradient" x1="0" x2="0" y1="0" y2="1">
                <stop offset="5%" stopColor="#1f8a70" stopOpacity={0.36} />
                <stop offset="95%" stopColor="#1f8a70" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid stroke="#e5e7eb" strokeDasharray="4 4" vertical={false} />
            <XAxis axisLine={false} dataKey="day" tickLine={false} />
            <YAxis axisLine={false} domain={[0, 100]} tickLine={false} />
            <Tooltip formatter={(value) => [`${value}%`, 'Dung lich']} />
            <Area
              dataKey="adherence"
              fill="url(#adherenceGradient)"
              stroke="#1f8a70"
              strokeWidth={3}
              type="monotone"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
