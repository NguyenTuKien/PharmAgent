import { Activity, BellRing, ShieldCheck } from 'lucide-react'
import { useState } from 'react'

import { AdherenceChart } from '../components/charts/AdherenceChart.jsx'
import { DateTimeField } from '../components/ui/DateTimeField.jsx'

export function DashboardPage() {
  const [nextDoseAt, setNextDoseAt] = useState(() => {
    const next = new Date()
    next.setHours(8, 0, 0, 0)
    return next
  })

  return (
    <div className="page-stack">
      <section className="summary-grid">
        <article className="metric-card">
          <Activity size={22} />
          <span>Lich hom nay</span>
          <strong>4 lieu</strong>
        </article>
        <article className="metric-card">
          <ShieldCheck size={22} />
          <span>Canh bao ton kho</span>
          <strong>2 thuoc</strong>
        </article>
        <article className="metric-card">
          <BellRing size={22} />
          <span>Thong bao moi</span>
          <strong>6</strong>
        </article>
      </section>

      <section className="dashboard-grid">
        <AdherenceChart />
        <article className="work-panel">
          <p className="eyebrow">Date/time picker</p>
          <h2>Len lich nhac tiep theo</h2>
          <p>
            Component nay dung chung cho cac form tao don thuoc, lieu uong va nhac lich o cac
            phase sau.
          </p>
          <DateTimeField label="Thoi diem nhac" value={nextDoseAt} onChange={setNextDoseAt} />
        </article>
      </section>
    </div>
  )
}
