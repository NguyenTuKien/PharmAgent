import {
  CalendarClock,
  CheckCircle2,
  Clock3,
  Filter,
  History,
  Loader2,
  Pill,
  RefreshCcw,
  Search,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'

import { Button } from '../components/ui/Button.jsx'
import { getApiErrorMessage } from '../lib/apiClient.js'
import { notify } from '../lib/toast.js'
import { useAuthStore } from '../modules/auth/authStore.js'
import {
  asPageContent,
  canFetchPillById,
  getDoseEvents,
  getMedications,
  getPillById,
  isPharmacityUrl,
  normalizePillId,
} from '../modules/medication/medicationApi.js'
import '../styles/caregiver/schedule.css'

const HISTORY_STATUS_META = {
  TAKEN: {
    label: 'Đã uống',
    tone: 'emerald',
  },
  OVERDUE: {
    label: 'Uống muộn',
    tone: 'amber',
  },
}

const HISTORY_STATUSES = Object.keys(HISTORY_STATUS_META)

const MEAL_LABELS = {
  BEFORE_MEAL: 'Trước bữa ăn',
  AFTER_MEAL: 'Sau bữa ăn',
  WITH_MEAL: 'Trong bữa ăn',
  ANYTIME: 'Bất kỳ lúc nào',
  BEFORE_SLEEP: 'Trước khi ngủ',
}

const SCHEDULE_LABELS = {
  DAILY: 'Hằng ngày',
  WEEKLY: 'Theo tuần',
  INTERVAL: 'Cách ngày',
  MONTHLY: 'Theo tháng',
  AS_NEEDED: 'Khi cần',
}

const fieldClass =
  'min-h-11 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-bold text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-teal-500 focus:ring-4 focus:ring-teal-100 disabled:bg-slate-50 disabled:text-slate-400'

function cx(...classes) {
  return classes.filter(Boolean).join(' ')
}

function localDateInput(date = new Date()) {
  const next = new Date(date)
  next.setMinutes(next.getMinutes() - next.getTimezoneOffset())
  return next.toISOString().slice(0, 10)
}

function dateFromInput(value) {
  if (!value) {
    return null
  }

  const [year, month, day] = String(value).split('-').map(Number)
  if (!year || !month || !day) {
    return null
  }

  return new Date(year, month - 1, day)
}

function addDays(value, amount) {
  const date = dateFromInput(value) ?? new Date()
  date.setDate(date.getDate() + amount)
  return localDateInput(date)
}

function dateKeyFromDateTime(value) {
  const text = String(value ?? '')
  return text.includes('T') ? text.split('T')[0] : text.slice(0, 10)
}

function dateFromDateTime(value) {
  return dateFromInput(dateKeyFromDateTime(value))
}

function normalizeTime(value) {
  if (!value) {
    return ''
  }
  return String(value).slice(0, 5)
}

function timeFromDateTime(value) {
  const text = String(value ?? '')
  const time = text.includes('T') ? text.split('T')[1] : text
  return normalizeTime(time)
}

function formatDate(value) {
  const date = value instanceof Date ? value : dateFromInput(value)
  if (!date) {
    return 'Chưa đặt'
  }

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

function formatDateTime(value) {
  if (!value) {
    return 'Chưa ghi nhận giờ'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return 'Chưa ghi nhận giờ'
  }

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function formatWeekday(date) {
  return new Intl.DateTimeFormat('vi-VN', { weekday: 'long' }).format(date)
}

function dateSectionTitle(value) {
  const date = value instanceof Date ? value : dateFromInput(value)
  if (!date) {
    return 'Ngày chưa xác định'
  }

  const today = dateFromInput(localDateInput())
  const yesterday = dateFromInput(addDays(localDateInput(), -1))
  const current = dateFromInput(localDateInput(date))

  if (current?.getTime() === today?.getTime()) {
    return 'Ngày hôm nay'
  }
  if (current?.getTime() === yesterday?.getTime()) {
    return 'Ngày hôm qua'
  }
  return `Ngày ${formatDate(date)}`
}

function medicationDoseTimes(schedule) {
  return schedule?.medDoses ?? schedule?.times ?? schedule?.scheduleTimeList ?? []
}

function doseTime(dose) {
  return normalizeTime(dose?.takenTime ?? dose?.timeOfDay)
}

function doseAmount(dose, medication) {
  return dose?.quantity ?? dose?.doseAmount ?? medication?.dosageAmount ?? 1
}

function scheduleType(schedule) {
  return schedule?.scheduleType ?? schedule?.frequencyType ?? 'DAILY'
}

function scheduleInterval(schedule) {
  return Number.parseInt(schedule?.frequencyInterval ?? schedule?.interval ?? 1, 10) || 1
}

function scheduleLabel(schedule) {
  const type = scheduleType(schedule)
  const interval = scheduleInterval(schedule)

  if (type === 'INTERVAL') {
    return `Mỗi ${interval} ngày`
  }
  if (type === 'WEEKLY') {
    return schedule?.daysOfWeek?.length
      ? `${SCHEDULE_LABELS[type]} - ${schedule.daysOfWeek.join(', ')}`
      : SCHEDULE_LABELS[type]
  }

  return SCHEDULE_LABELS[type] ?? type
}

function pillName(pill, fallback = 'Thuốc chưa đặt tên') {
  return pill?.name || pill?.display_name || pill?.title || fallback
}

function pillActiveIngredient(pill) {
  return pill?.activeIngredient || pill?.active_ingredient || ''
}

function pillManufacturer(pill) {
  return pill?.manufacturer || pill?.manufacturer_name || pill?.brand_name || ''
}

function usablePillImageUrl(value) {
  const url = typeof value === 'string' ? value.trim() : ''
  return url && !isPharmacityUrl(url) ? url : ''
}

function primaryPillImage(pill) {
  const images = pill?.images ?? []
  return [
    images.find((image) => image.isPrimary)?.imageUrl ||
      images.find((image) => image.isPrimary)?.image_url,
    images[0]?.imageUrl ||
      images[0]?.image_url ||
      images[0]?.url,
    pill?.imageUrls?.[0] ||
      pill?.primary_image_url ||
      pill?.primaryImageUrl ||
      pill?.image_url ||
      pill?.imageUrl,
  ].map(usablePillImageUrl).find(Boolean) ?? ''
}

function medicationName(medication, pill) {
  return medication?.nickname || pillName(pill, 'Thuốc trong hồ sơ')
}

function fullName(profile) {
  return [profile?.firstName, profile?.lastName].filter(Boolean).join(' ').trim() || 'Hồ sơ của tôi'
}

function findScheduleForEvent(medication, doseEvent) {
  const schedules = medication?.schedules ?? []
  return schedules.find((schedule) => schedule.id === doseEvent.scheduleId) ?? schedules[0] ?? {}
}

function findDoseForEvent(schedule, doseEvent) {
  const scheduledTime = timeFromDateTime(doseEvent.scheduledAt)
  const doses = medicationDoseTimes(schedule)
  return (
    doses.find((dose) => dose.id === doseEvent.medDoseId) ??
    doses.find((dose) => doseTime(dose) === scheduledTime) ??
    {}
  )
}

function matchHistorySearch(entry, searchText) {
  if (!searchText) {
    return true
  }

  const haystack = [
    medicationName(entry.medication, entry.pill),
    pillActiveIngredient(entry.pill),
    pillManufacturer(entry.pill),
    entry.medication?.purpose,
    entry.medication?.prescribedBy,
    entry.medication?.instruction,
    entry.schedule?.note,
    entry.event?.note,
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()

  return haystack.includes(searchText)
}

function groupEntriesByDate(entries) {
  const groups = new Map()

  entries.forEach((entry) => {
    if (!groups.has(entry.dateKey)) {
      groups.set(entry.dateKey, {
        date: entry.date,
        dateKey: entry.dateKey,
        entries: [],
      })
    }
    groups.get(entry.dateKey).entries.push(entry)
  })

  return [...groups.values()]
}

function PillThumb({ pill }) {
  const imageUrl = primaryPillImage(pill)

  return (
    <span className="grid h-12 w-12 shrink-0 place-items-center overflow-hidden rounded-lg border border-slate-200 bg-white text-teal-700 shadow-sm">
      {imageUrl ? (
        <img alt={pillName(pill, 'Ảnh thuốc')} className="h-full w-full object-cover" src={imageUrl} />
      ) : (
        <Pill size={22} />
      )}
    </span>
  )
}

function SummaryCard({ icon: Icon, label, value, tone = 'teal' }) {
  return (
    <article className={cx('schedule-summary-card', `schedule-summary-card--${tone}`)}>
      <span className="schedule-summary-card__icon">
        <Icon size={22} />
      </span>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  )
}

function DoseStatusBadge({ status }) {
  const meta = HISTORY_STATUS_META[status] ?? HISTORY_STATUS_META.TAKEN

  return (
    <span className={cx('schedule-dose-status', `schedule-dose-status--${meta.tone}`)}>
      {meta.label}
    </span>
  )
}

function DoseHistoryCard({ entry }) {
  const { dose, event, medication, pill, schedule, scheduledTime } = entry
  const statusMeta = HISTORY_STATUS_META[event.status] ?? HISTORY_STATUS_META.TAKEN
  const confirmationLabel = event.status === 'OVERDUE' ? 'Xác nhận uống muộn' : 'Xác nhận đã uống'

  return (
    <article className="schedule-card dose-history-card">
      <div className="schedule-card__time">
        {event.status === 'OVERDUE' ? <Clock3 size={18} /> : <CheckCircle2 size={18} />}
        <strong>{scheduledTime || '--:--'}</strong>
        <span>{doseAmount(dose, medication)} {medication?.dosageUnit ?? 'đơn vị'}</span>
      </div>

      <div className="schedule-card__body">
        <div className="flex min-w-0 items-start gap-3">
          <PillThumb pill={pill} />
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="truncate text-lg font-black text-slate-950">{medicationName(medication, pill)}</h3>
              <DoseStatusBadge status={event.status} />
            </div>
            <p className="truncate text-sm font-bold text-slate-500">
              {pillActiveIngredient(pill) || pillManufacturer(pill) || pillName(pill, 'Thuốc trong hồ sơ')}
            </p>
          </div>
        </div>

        <div className={cx('dose-history-confirmation', `dose-history-confirmation--${statusMeta.tone}`)}>
          <strong>{confirmationLabel}</strong>
          <span>{formatDateTime(event.takenAt)}</span>
        </div>

        <dl className="schedule-card__facts">
          <div>
            <dt>Giờ lịch</dt>
            <dd>{formatDate(entry.date)} · {scheduledTime || '--:--'}</dd>
          </div>
          <div>
            <dt>Liều dùng</dt>
            <dd>{doseAmount(dose, medication)} {medication?.dosageUnit ?? 'đơn vị'}</dd>
          </div>
          <div>
            <dt>Tần suất</dt>
            <dd>{scheduleLabel(schedule)}</dd>
          </div>
          <div>
            <dt>Bữa ăn</dt>
            <dd>{MEAL_LABELS[medication?.mealRelation] ?? medication?.mealRelation ?? 'Theo chỉ dẫn'}</dd>
          </div>
        </dl>

        <div className="schedule-card__notes">
          <span>{event.note || schedule?.note || medication?.instruction || medication?.purpose || 'Không có ghi chú thêm'}</span>
        </div>
      </div>
    </article>
  )
}

function HistoryEmptyState({ loading }) {
  if (loading) {
    return (
      <div className="schedule-empty-state" role="status">
        <Loader2 className="animate-spin text-teal-700" size={28} />
        <strong>Đang tải lịch sử uống thuốc</strong>
        <span>Hệ thống đang lấy các cữ thuốc đã được xác nhận.</span>
      </div>
    )
  }

  return (
    <div className="schedule-empty-state">
      <History size={30} />
      <strong>Chưa có cữ thuốc đã xác nhận</strong>
      <span>Thay đổi khoảng ngày hoặc quay lại trang lịch uống thuốc để xác nhận các cữ đã uống.</span>
    </div>
  )
}

export function DoseHistoryPage() {
  const activeProfile = useAuthStore((state) => state.activeProfile)
  const patientId = activeProfile?.id
  const today = localDateInput()
  const [dateFrom, setDateFrom] = useState(() => addDays(today, -30))
  const [dateTo, setDateTo] = useState(today)
  const [statusFilter, setStatusFilter] = useState('all')
  const [searchText, setSearchText] = useState('')
  const [doseEvents, setDoseEvents] = useState([])
  const [medications, setMedications] = useState([])
  const [pillMap, setPillMap] = useState({})
  const [loading, setLoading] = useState(false)

  const hydratePills = useCallback(async (items) => {
    const ids = [...new Set(items.map((item) => normalizePillId(item.pillId)).filter(canFetchPillById))]
    const missingIds = ids.filter((id) => !pillMap[id])

    if (!missingIds.length) {
      return
    }

    const results = await Promise.allSettled(missingIds.map((id) => getPillById(id)))
    const nextMap = {}

    results.forEach((result) => {
      if (result.status === 'fulfilled' && result.value?.id) {
        nextMap[normalizePillId(result.value.id)] = result.value
      }
    })

    if (Object.keys(nextMap).length) {
      setPillMap((current) => ({ ...current, ...nextMap }))
    }
  }, [pillMap])

  const loadHistory = useCallback(async () => {
    if (!patientId) {
      setDoseEvents([])
      setMedications([])
      return
    }

    setLoading(true)
    try {
      const [eventsPage, medicationsPage] = await Promise.all([
        getDoseEvents({ patientId, startDate: dateFrom, endDate: dateTo, page: 0, size: 1000 }),
        getMedications({ patientId, page: 0, size: 200 }),
      ])
      const nextMedications = asPageContent(medicationsPage)

      setDoseEvents(asPageContent(eventsPage))
      setMedications(nextMedications)
      await hydratePills(nextMedications)
    } catch (error) {
      notify.error('Không thể tải lịch sử uống thuốc', {
        description: getApiErrorMessage(error),
      })
    } finally {
      setLoading(false)
    }
  }, [dateFrom, dateTo, hydratePills, patientId])

  useEffect(() => {
    loadHistory()
  }, [loadHistory])

  const medicationMap = useMemo(() => {
    const nextMap = new Map()
    medications.forEach((medication) => {
      if (medication?.id) {
        nextMap.set(medication.id, medication)
      }
    })
    return nextMap
  }, [medications])

  const historyEntries = useMemo(() => {
    const search = searchText.trim().toLowerCase()

    return doseEvents
      .filter((event) => HISTORY_STATUSES.includes(event.status))
      .filter((event) => event.confirmedBy === patientId)
      .map((event) => {
        const medication = medicationMap.get(event.medicationId) ?? {
          id: event.medicationId,
          dosageUnit: 'đơn vị',
          schedules: [],
        }
        const schedule = findScheduleForEvent(medication, event)
        const dose = findDoseForEvent(schedule, event)
        const pill = pillMap[normalizePillId(medication.pillId)]
        const date = dateFromDateTime(event.scheduledAt) ?? new Date()

        return {
          date,
          dateKey: localDateInput(date),
          dose,
          event,
          medication,
          pill,
          schedule,
          scheduledTime: timeFromDateTime(event.scheduledAt),
        }
      })
      .filter((entry) => statusFilter === 'all' || entry.event.status === statusFilter)
      .filter((entry) => matchHistorySearch(entry, search))
      .sort((first, second) => (
        second.dateKey.localeCompare(first.dateKey) ||
        second.scheduledTime.localeCompare(first.scheduledTime) ||
        medicationName(first.medication, first.pill).localeCompare(medicationName(second.medication, second.pill))
      ))
  }, [doseEvents, medicationMap, patientId, pillMap, searchText, statusFilter])

  const groupedEntries = useMemo(() => groupEntriesByDate(historyEntries), [historyEntries])

  const summary = useMemo(() => {
    const taken = historyEntries.filter((entry) => entry.event.status === 'TAKEN').length
    const overdue = historyEntries.filter((entry) => entry.event.status === 'OVERDUE').length
    const total = historyEntries.length
    const onTimeRate = total ? Math.round((taken / total) * 100) : 0

    return {
      onTimeRate: `${onTimeRate}%`,
      overdue,
      taken,
      total,
    }
  }, [historyEntries])

  const setQuickRange = (type) => {
    const currentToday = localDateInput()

    if (type === 'today') {
      setDateFrom(currentToday)
      setDateTo(currentToday)
    } else if (type === 'week') {
      setDateFrom(addDays(currentToday, -6))
      setDateTo(currentToday)
    } else if (type === 'month') {
      const date = dateFromInput(currentToday)
      setDateFrom(localDateInput(new Date(date.getFullYear(), date.getMonth(), 1)))
      setDateTo(currentToday)
    } else {
      setDateFrom(addDays(currentToday, -30))
      setDateTo(currentToday)
    }
  }

  return (
    <div className="caregiver-schedule-page mx-auto grid w-full max-w-[1500px] gap-4 pb-4 text-slate-950 sm:gap-5 lg:pb-6 xl:gap-6">
      <section className="schedule-hero">
        <div className="min-w-0">
          <p>Lịch sử uống thuốc</p>
          <h1>Lịch sử xác nhận uống thuốc</h1>
          <span>
            Theo dõi các cữ thuốc {fullName(activeProfile)} đã xác nhận là đã uống hoặc uống muộn.
          </span>
        </div>
        <div className="schedule-hero__actions">
          <Button disabled={loading} variant="ghost" onClick={loadHistory}>
            {loading ? <Loader2 className="animate-spin" size={17} /> : <RefreshCcw size={17} />}
            Làm mới
          </Button>
        </div>
      </section>

      <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard icon={History} label="Đã xác nhận" value={summary.total} />
        <SummaryCard icon={CheckCircle2} label="Đã uống" tone="emerald" value={summary.taken} />
        <SummaryCard icon={Clock3} label="Uống muộn" tone={summary.overdue ? 'amber' : 'slate'} value={summary.overdue} />
        <SummaryCard icon={CalendarClock} label="Đúng giờ" tone="blue" value={summary.onTimeRate} />
      </section>

      <section className="schedule-filter-panel">
        <div className="schedule-filter-panel__title">
          <Filter size={18} />
          <strong>Bộ lọc lịch sử</strong>
        </div>

        <div className="grid gap-3 lg:grid-cols-[minmax(170px,0.8fr)_minmax(170px,0.8fr)_minmax(180px,0.8fr)_minmax(240px,1.3fr)]">
          <label className="schedule-filter-field">
            Từ ngày
            <input className={fieldClass} type="date" value={dateFrom} onChange={(event) => setDateFrom(event.target.value)} />
          </label>
          <label className="schedule-filter-field">
            Đến ngày
            <input className={fieldClass} type="date" value={dateTo} onChange={(event) => setDateTo(event.target.value)} />
          </label>
          <label className="schedule-filter-field">
            Trạng thái
            <select className={fieldClass} value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="all">Tất cả đã xác nhận</option>
              <option value="TAKEN">Đã uống</option>
              <option value="OVERDUE">Uống muộn</option>
            </select>
          </label>
          <label className="schedule-filter-field">
            Tìm kiếm
            <span className="schedule-search-field">
              <Search size={18} />
              <input
                placeholder="Tên thuốc, hoạt chất, bác sĩ, ghi chú..."
                value={searchText}
                onChange={(event) => setSearchText(event.target.value)}
              />
            </span>
          </label>
        </div>

        <div className="schedule-quick-filters">
          {[
            { value: 'today', label: 'Hôm nay' },
            { value: 'week', label: '7 ngày' },
            { value: 'default', label: '30 ngày' },
            { value: 'month', label: 'Tháng này' },
          ].map((item) => (
            <button key={item.value} type="button" onClick={() => setQuickRange(item.value)}>
              {item.label}
            </button>
          ))}
        </div>
      </section>

      <section className="schedule-timeline">
        {groupedEntries.length ? (
          groupedEntries.map((group) => (
            <section className="schedule-day-section" key={group.dateKey}>
              <div className="schedule-section-heading">
                <div>
                  <p>{formatWeekday(group.date)}</p>
                  <h2>{dateSectionTitle(group.date)}</h2>
                </div>
                <span>{group.entries.length} cữ thuốc</span>
              </div>

              <div className="grid gap-4 xl:grid-cols-2">
                {group.entries.map((entry) => (
                  <DoseHistoryCard entry={entry} key={entry.event.id ?? `${entry.event.medicationId}:${entry.event.scheduledAt}`} />
                ))}
              </div>
            </section>
          ))
        ) : (
          <HistoryEmptyState loading={loading} />
        )}
      </section>
    </div>
  )
}

export default DoseHistoryPage
