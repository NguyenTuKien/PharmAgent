import {
  CalendarClock,
  CalendarDays,
  CheckCircle2,
  CircleAlert,
  Clock3,
  Filter,
  Loader2,
  PackageCheck,
  Pill,
  Plus,
  RefreshCcw,
  Search,
  Trash2,
  UserRound,
  X,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'

import { Button } from '../components/ui/Button.jsx'
import { ConfirmDialog } from '../components/ui/Modal.jsx'
import { getApiErrorMessage } from '../lib/apiClient.js'
import { notify } from '../lib/toast.js'
import { useAuthStore } from '../modules/auth/authStore.js'
import {
  asPageContent,
  createCaregiverMedication,
  canFetchPillById,
  deleteCaregiverMedication,
  getDoseEvents,
  getMedications,
  getPillById,
  isPharmacityUrl,
  normalizePillId,
  updateCaregiverMedication,
  updateElderlyDoseStatus,
} from '../modules/medication/medicationApi.js'
import {
  getCaregiverRelationships,
  getProfiles,
} from '../modules/profile/profileApi.js'
import {
  MedicationFormModal,
} from './MedicationsPage.jsx'
import '../styles/caregiver/schedule.css'

const SCHEDULE_OPTIONS = [
  { value: 'DAILY', label: 'Hằng ngày' },
  { value: 'WEEKLY', label: 'Theo tuần' },
  { value: 'INTERVAL', label: 'Cách ngày' },
  { value: 'MONTHLY', label: 'Theo tháng' },
  { value: 'AS_NEEDED', label: 'Khi cần' },
]

const SCHEDULE_LABELS = SCHEDULE_OPTIONS.reduce((labels, option) => {
  labels[option.value] = option.label
  return labels
}, {})

const MEAL_LABELS = {
  BEFORE_MEAL: 'Trước bữa ăn',
  AFTER_MEAL: 'Sau bữa ăn',
  WITH_MEAL: 'Trong bữa ăn',
  ANYTIME: 'Bất kỳ lúc nào',
  BEFORE_SLEEP: 'Trước khi ngủ',
}

const DOSE_STATUS_META = {
  PENDING: {
    label: 'Chờ xác nhận',
    tone: 'slate',
  },
  TAKEN: {
    label: 'Đã uống',
    tone: 'emerald',
  },
  MISSED: {
    label: 'Chưa uống',
    tone: 'rose',
  },
  SKIPPED: {
    label: 'Bỏ qua',
    tone: 'amber',
  },
  OVERDUE: {
    label: 'Uống muộn',
    tone: 'amber',
  },
}

const ELDERLY_DOSE_STATUS_OPTIONS = [
  {
    value: 'TAKEN',
    label: 'Đã uống',
    description: 'Xác nhận đã uống đúng lịch hoặc vừa uống.',
    icon: CheckCircle2,
  },
  {
    value: 'MISSED',
    label: 'Chưa uống',
    description: 'Báo cho người chăm sóc biết cữ này chưa được uống.',
    icon: CircleAlert,
  },
  {
    value: 'OVERDUE',
    label: 'Uống muộn',
    description: 'Ghi nhận đã uống nhưng trễ hơn giờ nhắc.',
    icon: Clock3,
  },
]

const WEEKDAY_VALUES = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']

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

function localDateTimePayload(date = new Date()) {
  const next = new Date(date)
  next.setMinutes(next.getMinutes() - next.getTimezoneOffset())
  return next.toISOString().slice(0, 19)
}

function addDays(value, amount) {
  const date = dateFromInput(value) ?? new Date()
  date.setDate(date.getDate() + amount)
  return localDateInput(date)
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

function normalizeDate(date) {
  const next = new Date(date)
  next.setHours(0, 0, 0, 0)
  return next
}

function dateKey(date) {
  return localDateInput(date)
}

function dayDiff(firstDate, secondDate) {
  const first = normalizeDate(firstDate).getTime()
  const second = normalizeDate(secondDate).getTime()
  return Math.round((second - first) / 86_400_000)
}

function monthDiff(firstDate, secondDate) {
  return (
    (secondDate.getFullYear() - firstDate.getFullYear()) * 12 +
    secondDate.getMonth() -
    firstDate.getMonth()
  )
}

function enumerateDates(startValue, endValue) {
  const start = normalizeDate(dateFromInput(startValue) ?? new Date())
  const end = normalizeDate(dateFromInput(endValue) ?? start)
  const safeEnd = end < start ? start : end
  const days = Math.min(dayDiff(start, safeEnd), 62)

  return Array.from({ length: days + 1 }, (_, index) => {
    const date = new Date(start)
    date.setDate(start.getDate() + index)
    return date
  })
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

function formatWeekday(date) {
  return new Intl.DateTimeFormat('vi-VN', { weekday: 'long' }).format(date)
}

function dateSectionTitle(value) {
  const date = value instanceof Date ? value : dateFromInput(value)
  const today = normalizeDate(new Date())
  const tomorrow = new Date(today)
  tomorrow.setDate(today.getDate() + 1)

  if (date && normalizeDate(date).getTime() === today.getTime()) {
    return 'Ngày hôm nay'
  }
  if (date && normalizeDate(date).getTime() === tomorrow.getTime()) {
    return 'Ngày mai'
  }
  return `Ngày ${formatDate(date)}`
}

function normalizeTime(value) {
  if (!value) {
    return ''
  }
  return String(value).slice(0, 5)
}

function doseTime(dose) {
  return normalizeTime(dose?.takenTime ?? dose?.timeOfDay)
}

function doseAmount(dose, medication) {
  return dose?.quantity ?? dose?.doseAmount ?? medication?.dosageAmount ?? 1
}

function dateKeyFromDateTime(value) {
  const text = String(value ?? '')
  return text.includes('T') ? text.split('T')[0] : text.slice(0, 10)
}

function timeFromDateTime(value) {
  const text = String(value ?? '')
  const time = text.includes('T') ? text.split('T')[1] : text
  return normalizeTime(time)
}

function medicationDoseTimes(schedule) {
  return schedule?.medDoses ?? schedule?.times ?? schedule?.scheduleTimeList ?? []
}

function doseEventKey({ medicationId, scheduleId, medDoseId, scheduledAt }) {
  return [
    medicationId,
    scheduleId ?? '',
    medDoseId ?? '',
    dateKeyFromDateTime(scheduledAt),
    timeFromDateTime(scheduledAt),
  ].join(':')
}

function entryDoseEventKey(entry) {
  return doseEventKey({
    medicationId: entry.medication?.id,
    scheduleId: entry.schedule?.id,
    medDoseId: entry.dose?.id,
    scheduledAt: `${entry.dateKey}T${entry.time}:00`,
  })
}

function patientId(profile) {
  return profile?.profileId ?? profile?.id
}

function fullName(profile) {
  return [profile?.firstName, profile?.lastName].filter(Boolean).join(' ').trim() || 'Hồ sơ PharmAgent'
}

function patientInitials(profile) {
  const first = profile?.firstName?.trim()?.[0] ?? ''
  const last = profile?.lastName?.trim()?.[0] ?? ''
  return `${first}${last}`.toUpperCase() || 'PA'
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

function oneDecimalInput(value, fallback = '1.0') {
  const amount = Number.parseFloat(value)
  return Number.isFinite(amount) && amount > 0 ? amount.toFixed(1) : fallback
}

function localId(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function toApiTime(value) {
  const time = normalizeTime(value)
  return time.length === 5 ? `${time}:00` : time
}

function optionalText(value) {
  const trimmed = typeof value === 'string' ? value.trim() : ''
  return trimmed || null
}

function newDoseTime(timeOfDay = '08:00', amount = '1.0') {
  return {
    localId: localId('dose'),
    timeOfDay,
    doseAmount: oneDecimalInput(amount),
  }
}

function newSchedule(startDate = localDateInput(), amount = '1.0') {
  return {
    localId: localId('schedule'),
    frequencyType: 'DAILY',
    interval: '1',
    daysOfWeek: [],
    reminderEnabled: true,
    reminderMinutesBefore: '15',
    note: '',
    startDate,
    endDate: '',
    times: [newDoseTime('08:00', oneDecimalInput(amount))],
  }
}

function emptyMedicationForm(patientProfile) {
  const startDate = localDateInput()

  return {
    patientId: patientId(patientProfile) ?? '',
    pillId: '',
    selectedPill: null,
    pillQuery: '',
    nickname: '',
    dosageAmount: '1',
    dosageUnit: 'Viên',
    route: 'Uống',
    mealRelation: 'AFTER_MEAL',
    instruction: '',
    prescribedBy: '',
    purpose: '',
    startDate,
    endDate: '',
    totalQuantity: '30',
    schedules: [newSchedule(startDate, '1.0')],
  }
}

function medicationToForm(medication, patientProfile, pill) {
  const startDate = medication?.startDate ?? localDateInput()
  const normalizedPillId = normalizePillId(medication?.pillId)
  const schedules = (medication?.schedules ?? []).map((schedule) => ({
    localId: schedule.id ?? localId('schedule'),
    id: schedule.id,
    frequencyType: schedule.scheduleType ?? schedule.frequencyType ?? 'DAILY',
    interval: String(schedule.frequencyInterval ?? schedule.interval ?? 1),
    daysOfWeek: schedule.daysOfWeek ?? [],
    reminderEnabled: schedule.reminderEnabled ?? true,
    reminderMinutesBefore: String(schedule.reminderMinutesBefore ?? 15),
    note: schedule.note ?? '',
    startDate: schedule.startDate ?? startDate,
    endDate: schedule.endDate ?? '',
    times: medicationDoseTimes(schedule).map((dose) => ({
      localId: dose.id ?? localId('dose'),
      id: dose.id,
      timeOfDay: doseTime(dose),
      doseAmount: oneDecimalInput(doseAmount(dose, medication) || medication?.dosageAmount || '1'),
    })),
  }))

  return {
    patientId: medication?.patientId ?? patientId(patientProfile) ?? '',
    pillId: canFetchPillById(normalizedPillId) ? normalizedPillId : '',
    selectedPill: pill ?? null,
    pillQuery: pillName(pill, ''),
    nickname: medication?.nickname ?? '',
    dosageAmount: String(medication?.dosageAmount ?? '1'),
    dosageUnit: medication?.dosageUnit ?? 'Viên',
    route: medication?.route ?? 'Uống',
    mealRelation: medication?.mealRelation ?? 'AFTER_MEAL',
    instruction: medication?.instruction ?? '',
    prescribedBy: medication?.prescribedBy ?? '',
    purpose: medication?.purpose ?? '',
    startDate,
    endDate: medication?.endDate ?? '',
    totalQuantity: String(medication?.totalQuantity ?? '30'),
    schedules: schedules.length ? schedules : [newSchedule(startDate, oneDecimalInput(medication?.dosageAmount ?? '1'))],
  }
}

function buildMedicationPayload(form) {
  return {
    patientId: form.patientId,
    pillId: normalizePillId(form.pillId),
    nickname: optionalText(form.nickname),
    dosageAmount: Number(form.dosageAmount),
    dosageUnit: form.dosageUnit,
    route: form.route,
    mealRelation: form.mealRelation,
    instruction: optionalText(form.instruction),
    prescribedBy: optionalText(form.prescribedBy),
    purpose: optionalText(form.purpose),
    startDate: form.startDate,
    endDate: form.endDate || null,
    totalQuantity: Number.parseInt(form.totalQuantity, 10),
    schedules: form.schedules.map((schedule) => ({
      id: schedule.id,
      scheduleType: schedule.frequencyType,
      frequencyType: schedule.frequencyType,
      frequencyInterval: Number.parseInt(schedule.interval, 10) || 1,
      interval: Number.parseInt(schedule.interval, 10) || 1,
      daysOfWeek: schedule.frequencyType === 'WEEKLY' ? schedule.daysOfWeek : [],
      reminderEnabled: Boolean(schedule.reminderEnabled),
      reminderMinutesBefore: Number.parseInt(schedule.reminderMinutesBefore, 10) || 0,
      note: optionalText(schedule.note),
      startDate: schedule.startDate || form.startDate,
      endDate: schedule.endDate || form.endDate || null,
      isActive: true,
      medDoseRequests: schedule.times.map((dose) => ({
        id: dose.id,
        takenTime: toApiTime(dose.timeOfDay),
        timeOfDay: toApiTime(dose.timeOfDay),
        quantity: Number(dose.doseAmount || form.dosageAmount || 1),
        doseAmount: Number(dose.doseAmount || form.dosageAmount || 1),
      })),
      times: schedule.times.map((dose) => ({
        id: dose.id,
        takenTime: toApiTime(dose.timeOfDay),
        timeOfDay: toApiTime(dose.timeOfDay),
        quantity: Number(dose.doseAmount || form.dosageAmount || 1),
        doseAmount: Number(dose.doseAmount || form.dosageAmount || 1),
      })),
    })),
  }
}

function validateMedicationForm(form) {
  if (!form.patientId) {
    return 'Chọn hồ sơ người thân trước khi lưu thuốc'
  }
  if (!form.pillId) {
    return 'Chọn thuốc từ danh mục trước khi tiếp tục'
  }
  if (!Number(form.dosageAmount) || Number(form.dosageAmount) <= 0) {
    return 'Liều lượng mỗi lần phải lớn hơn 0'
  }
  if (!Number.parseInt(form.totalQuantity, 10) || Number.parseInt(form.totalQuantity, 10) <= 0) {
    return 'Tổng số lượng thuốc phải lớn hơn 0'
  }
  if (form.endDate && form.startDate && form.endDate < form.startDate) {
    return 'Ngày kết thúc không được trước ngày bắt đầu'
  }
  if (!form.schedules.length) {
    return 'Cần ít nhất một lịch uống'
  }

  const invalidSchedule = form.schedules.find((schedule) => {
    if (schedule.endDate && schedule.startDate && schedule.endDate < schedule.startDate) {
      return true
    }
    if (schedule.frequencyType !== 'AS_NEEDED' && schedule.times.length === 0) {
      return true
    }
    return schedule.times.some((dose) => {
      const amount = Number(dose.doseAmount || form.dosageAmount)
      return !dose.timeOfDay || !Number.isFinite(amount) || amount <= 0
    })
  })

  return invalidSchedule ? 'Kiểm tra lại ngày, khung giờ và liều lượng trong lịch uống' : ''
}

function medicationName(medication, pill) {
  return medication?.nickname || pillName(pill)
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

  if (type === 'WEEKLY') {
    const days = schedule?.daysOfWeek?.length ? schedule.daysOfWeek.join(', ') : 'theo ngày bắt đầu'
    return `${SCHEDULE_LABELS[type]} - ${days}`
  }
  if (type === 'INTERVAL') {
    return `Mỗi ${interval} ngày`
  }

  return SCHEDULE_LABELS[type] ?? type
}

function mergeCaregiverPatients(localProfiles, acceptedRelationships) {
  const patientsById = new Map()

  acceptedRelationships.forEach((profile) => {
    const id = patientId(profile)
    if (!id) {
      return
    }
    patientsById.set(id, {
      ...profile,
      id,
      profileId: id,
      source: 'relationship',
      status: profile.status ?? 'ACCEPTED',
    })
  })

  localProfiles
    .filter((profile) => profile.role === 'ELDERLY')
    .forEach((profile) => {
      const id = patientId(profile)
      if (!id || patientsById.has(id)) {
        return
      }
      patientsById.set(id, {
        ...profile,
        id,
        profileId: id,
        source: 'local',
        status: 'LOCAL',
      })
    })

  return [...patientsById.values()]
}

function scheduleOccursOnDate(schedule, medication, date) {
  const type = scheduleType(schedule)
  if (type === 'AS_NEEDED') {
    return false
  }

  const start = dateFromInput(schedule?.startDate || medication?.startDate)
  const end = dateFromInput(schedule?.endDate || medication?.endDate)
  const current = normalizeDate(date)

  if (start && current < normalizeDate(start)) {
    return false
  }
  if (end && current > normalizeDate(end)) {
    return false
  }

  const anchor = normalizeDate(start ?? current)
  const interval = scheduleInterval(schedule)

  if (type === 'WEEKLY') {
    const days = schedule?.daysOfWeek?.length
      ? schedule.daysOfWeek
      : [WEEKDAY_VALUES[anchor.getDay()]]
    const weekDistance = Math.floor(dayDiff(anchor, current) / 7)
    return weekDistance >= 0 && weekDistance % interval === 0 && days.includes(WEEKDAY_VALUES[current.getDay()])
  }

  if (type === 'MONTHLY') {
    const distance = monthDiff(anchor, current)
    return distance >= 0 && distance % interval === 0 && anchor.getDate() === current.getDate()
  }

  const distance = dayDiff(anchor, current)
  return distance >= 0 && distance % interval === 0
}

function flattenScheduleEntries(patients, medicationMap, startDate, endDate) {
  const dates = enumerateDates(startDate, endDate)

  return patients.flatMap((patient) => {
    const currentPatientId = patientId(patient)
    const medications = medicationMap[currentPatientId] ?? []

    return medications.flatMap((medication) =>
      (medication.schedules ?? []).flatMap((schedule, scheduleIndex) => {
        const times = medicationDoseTimes(schedule)
          .map((dose, doseIndex) => ({
            dose,
            doseIndex,
            time: doseTime(dose),
          }))
          .filter((item) => item.time)

        if (!times.length) {
          return []
        }

        return dates.flatMap((date) => {
          if (!scheduleOccursOnDate(schedule, medication, date)) {
            return []
          }

          return times.map(({ dose, doseIndex, time }) => ({
            date,
            dateKey: dateKey(date),
            dose,
            doseIndex,
            key: [
              currentPatientId,
              medication.id,
              schedule.id ?? scheduleIndex,
              dose.id ?? doseIndex,
              dateKey(date),
              time,
            ].join(':'),
            medication,
            patient,
            schedule,
            scheduleIndex,
            time,
          }))
        })
      }),
    )
  }).sort((first, second) => (
    first.dateKey.localeCompare(second.dateKey) ||
    first.time.localeCompare(second.time) ||
    medicationName(first.medication).localeCompare(medicationName(second.medication))
  ))
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

function matchSearch(entry, pill, searchText) {
  if (!searchText) {
    return true
  }

  const haystack = [
    fullName(entry.patient),
    entry.medication.nickname,
    entry.medication.purpose,
    entry.medication.prescribedBy,
    pillName(pill, ''),
    pillActiveIngredient(pill),
    pillManufacturer(pill),
    entry.schedule.note,
    entry.medication.instruction,
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()

  return haystack.includes(searchText)
}

function PatientAvatar({ profile, size = 'md' }) {
  const sizeClass = size === 'lg' ? 'h-14 w-14 text-base' : 'h-11 w-11 text-sm'

  return (
    <span className={cx('grid shrink-0 place-items-center overflow-hidden rounded-lg bg-teal-100 font-black text-teal-800 ring-1 ring-teal-200', sizeClass)}>
      {profile?.avatarUrl ? <img alt="" className="h-full w-full object-cover" src={profile.avatarUrl} /> : patientInitials(profile)}
    </span>
  )
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
  const meta = DOSE_STATUS_META[status ?? 'PENDING'] ?? DOSE_STATUS_META.PENDING

  return (
    <span className={cx('schedule-dose-status', `schedule-dose-status--${meta.tone}`)}>
      {meta.label}
    </span>
  )
}

function ScheduleCard({ entry, eventDose, isElderly = false, pill, onDelete, onEdit, onFocus }) {
  const { dose, medication, patient, schedule, time } = entry
  const lowStock = Number(medication.totalQuantity ?? 0) <= 7
  const reminder = schedule.reminderEnabled === false
    ? 'Không nhắc'
    : `Nhắc trước ${schedule.reminderMinutesBefore ?? 0} phút`
  const status = eventDose?.status ?? 'PENDING'
  const openFocus = () => {
    if (isElderly) {
      onFocus?.(entry)
    }
  }
  const handleKeyDown = (event) => {
    if (!isElderly) {
      return
    }
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      openFocus()
    }
  }

  return (
    <article
      aria-label={isElderly ? `Mở cữ thuốc ${medicationName(medication, pill)} lúc ${time}` : undefined}
      className={cx('schedule-card', isElderly && 'schedule-card--interactive')}
      role={isElderly ? 'button' : undefined}
      tabIndex={isElderly ? 0 : undefined}
      onClick={openFocus}
      onKeyDown={handleKeyDown}
    >
      <div className="schedule-card__time">
        <Clock3 size={18} />
        <strong>{time}</strong>
        <span>{doseAmount(dose, medication)} {medication.dosageUnit ?? 'đơn vị'}</span>
      </div>

      <div className="schedule-card__body">
        <div className="flex min-w-0 items-start gap-3">
          <PillThumb pill={pill} />
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="truncate text-lg font-black text-slate-950">{medicationName(medication, pill)}</h3>
              {lowStock ? <span className="schedule-status schedule-status--warning">Sắp hết</span> : null}
              <DoseStatusBadge status={status} />
            </div>
            <p className="truncate text-sm font-bold text-slate-500">
              {pillActiveIngredient(pill) || pillManufacturer(pill) || pillName(pill, 'Thuốc trong hồ sơ')}
            </p>
          </div>
        </div>

        <div className="schedule-patient-strip">
          <PatientAvatar profile={patient} />
          <div className="min-w-0">
            <span>Người cần uống</span>
            <strong>{fullName(patient)}</strong>
          </div>
        </div>

        <dl className="schedule-card__facts">
          <div>
            <dt>Tần suất</dt>
            <dd>{scheduleLabel(schedule)}</dd>
          </div>
          <div>
            <dt>Bữa ăn</dt>
            <dd>{MEAL_LABELS[medication.mealRelation] ?? medication.mealRelation ?? 'Theo chỉ dẫn'}</dd>
          </div>
          <div>
            <dt>Thời gian</dt>
            <dd>{formatDate(schedule.startDate || medication.startDate)} - {formatDate(schedule.endDate || medication.endDate)}</dd>
          </div>
          <div>
            <dt>Tồn kho</dt>
            <dd>{medication.totalQuantity ?? 0} {medication.dosageUnit ?? 'đơn vị'}</dd>
          </div>
        </dl>

        <div className="schedule-card__notes">
          <span>{reminder}</span>
          <span>{schedule.note || medication.instruction || medication.purpose || 'Chưa có ghi chú riêng'}</span>
        </div>

        {isElderly ? (
          <div className="schedule-card__elderly-hint">Bấm để cập nhật tình trạng uống thuốc</div>
        ) : (
          <div className="schedule-card__actions">
            <button className="schedule-icon-button" title="Sửa thuốc và lịch" type="button" onClick={() => onEdit(medication, patient)}>
              <CalendarClock size={17} />
              <span>Sửa</span>
            </button>
            <button className="schedule-icon-button schedule-icon-button--danger" title="Xóa thuốc" type="button" onClick={() => onDelete(medication, patient)}>
              <Trash2 size={17} />
              <span>Xóa</span>
            </button>
          </div>
        )}
      </div>
    </article>
  )
}

function EmptyScheduleState({ canCreate, isElderly = false, loading, onCreate }) {
  if (loading) {
    return (
      <div className="schedule-empty-state" role="status">
        <Loader2 className="animate-spin text-teal-700" size={28} />
        <strong>Đang gom lịch uống thuốc</strong>
        <span>Hệ thống đang tải lịch từ các hồ sơ người thân đang chăm sóc.</span>
      </div>
    )
  }

  return (
    <div className="schedule-empty-state">
      <CalendarDays size={30} />
      <strong>Chưa có lịch uống trong khoảng ngày đã chọn</strong>
      <span>
        {isElderly
          ? 'Chỉnh khoảng ngày để xem thêm lịch, hoặc liên hệ caregiver nếu bạn chưa thấy thuốc cần uống.'
          : 'Thêm thuốc mới hoặc chỉnh filter ngày tháng để xem lịch uống đã tạo bên quản lý thuốc.'}
      </span>
      {canCreate ? (
        <Button variant="primary" onClick={onCreate}>
          <Plus size={17} />
          Tạo thuốc và lịch
        </Button>
      ) : null}
    </div>
  )
}

function ElderlyDoseFocusModal({ entry, eventDose, pill, savingStatus, onClose, onStatusChange }) {
  if (!entry) {
    return null
  }

  const { dose, medication, schedule, time } = entry
  const status = eventDose?.status ?? 'PENDING'
  const statusMeta = DOSE_STATUS_META[status] ?? DOSE_STATUS_META.PENDING
  const canUpdate = Boolean(eventDose?.id) && !savingStatus

  return (
    <div className="schedule-focus-layer" role="presentation" onMouseDown={onClose}>
      <section
        aria-label="Cập nhật cữ uống thuốc"
        aria-modal="true"
        className="schedule-focus-card"
        role="dialog"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button className="schedule-focus-card__close" title="Đóng" type="button" onClick={onClose}>
          <X size={22} />
        </button>

        <div className="schedule-focus-card__hero">
          <PillThumb pill={pill} />
          <div className="min-w-0">
            <p>{formatDate(entry.date)} · {time}</p>
            <h2>{medicationName(medication, pill)}</h2>
            <span>{doseAmount(dose, medication)} {medication.dosageUnit ?? 'đơn vị'} · {MEAL_LABELS[medication.mealRelation] ?? medication.mealRelation ?? 'Chưa đặt bữa ăn'}</span>
          </div>
        </div>

        <div className={cx('schedule-focus-card__status', `schedule-focus-card__status--${statusMeta.tone}`)}>
          <strong>{statusMeta.label}</strong>
          <span>{eventDose?.takenAt ? `Cập nhật lúc ${timeFromDateTime(eventDose.takenAt)}` : 'Trạng thái hiện tại của cữ thuốc này'}</span>
        </div>

        <dl className="schedule-focus-card__facts">
          <div>
            <dt>Tần suất</dt>
            <dd>{scheduleLabel(schedule)}</dd>
          </div>
          <div>
            <dt>Nhắc uống</dt>
            <dd>{schedule.reminderEnabled === false ? 'Không nhắc' : `Trước ${schedule.reminderMinutesBefore ?? 0} phút`}</dd>
          </div>
          <div>
            <dt>Chỉ dẫn</dt>
            <dd>{schedule.note || medication.instruction || medication.purpose || 'Chưa có ghi chú riêng'}</dd>
          </div>
        </dl>

        <div className="schedule-focus-card__options">
          {ELDERLY_DOSE_STATUS_OPTIONS.map((option) => {
            const Icon = option.icon
            const selected = status === option.value

            return (
              <button
                className={cx('schedule-dose-option', selected && 'schedule-dose-option--selected')}
                disabled={!canUpdate || selected}
                key={option.value}
                type="button"
                onClick={() => onStatusChange(option.value)}
              >
                <Icon size={22} />
                <span>
                  <strong>{option.label}</strong>
                  <small>{option.description}</small>
                </span>
              </button>
            )
          })}
        </div>

        {!eventDose?.id ? (
          <p className="schedule-focus-card__warning">
            Cữ thuốc này chưa có bản ghi đồng bộ nên chưa thể cập nhật trạng thái.
          </p>
        ) : null}
      </section>
    </div>
  )
}

function UnscheduledMedications({ items, pillMap, onCreate, onDelete, onEdit }) {
  if (!items.length) {
    return null
  }

  return (
    <section className="schedule-unscheduled">
      <div className="schedule-section-heading">
        <div>
          <p>Chưa có lịch cố định</p>
          <h2>Thuốc cần bổ sung lịch</h2>
        </div>
        <span>{items.length} thuốc</span>
      </div>

      <div className="grid gap-3 lg:grid-cols-2">
        {items.map(({ medication, patient }) => {
          const pill = pillMap[normalizePillId(medication.pillId)]
          return (
            <article className="schedule-missing-card" key={`${patientId(patient)}:${medication.id}`}>
              <div className="flex min-w-0 items-center gap-3">
                <PillThumb pill={pill} />
                <div className="min-w-0 flex-1">
                  <strong>{medicationName(medication, pill)}</strong>
                  <span>{fullName(patient)} - {medication.dosageAmount ?? 1} {medication.dosageUnit ?? 'đơn vị'}</span>
                </div>
              </div>
              <div className="schedule-card__actions">
                <button className="schedule-icon-button" type="button" onClick={() => onEdit(medication, patient)}>
                  <CalendarClock size={17} />
                  <span>Thêm lịch</span>
                </button>
                <button className="schedule-icon-button schedule-icon-button--danger" type="button" onClick={() => onDelete(medication, patient)}>
                  <Trash2 size={17} />
                  <span>Xóa</span>
                </button>
              </div>
            </article>
          )
        })}
      </div>

      <div className="schedule-inline-action">
        <Button variant="secondary" onClick={onCreate}>
          <Plus size={17} />
          Tạo thuốc mới
        </Button>
      </div>
    </section>
  )
}

export function SchedulePage() {
  const [searchParams] = useSearchParams()
  const activeProfile = useAuthStore((state) => state.activeProfile)
  const activeRole = activeProfile?.role?.toUpperCase?.()
  const isElderly = activeRole === 'ELDERLY'
  const [patients, setPatients] = useState([])
  const [patientsLoading, setPatientsLoading] = useState(true)
  const [medicationsByPatient, setMedicationsByPatient] = useState({})
  const [medicationsLoading, setMedicationsLoading] = useState(false)
  const [eventsLoading, setEventsLoading] = useState(false)
  const [doseEventsByKey, setDoseEventsByKey] = useState({})
  const [pillMap, setPillMap] = useState({})
  const [dateFrom, setDateFrom] = useState(() => localDateInput())
  const [dateTo, setDateTo] = useState(() => addDays(localDateInput(), 7))
  const [patientFilter, setPatientFilter] = useState(searchParams.get('profileId') ?? 'all')
  const [frequencyFilter, setFrequencyFilter] = useState('all')
  const [statusFilter, setStatusFilter] = useState('active')
  const [searchText, setSearchText] = useState(searchParams.get('q') ?? '')
  const [drawerMode, setDrawerMode] = useState(null)
  const [form, setForm] = useState(() => emptyMedicationForm())
  const [saving, setSaving] = useState(false)
  const [editingMedication, setEditingMedication] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [focusedEntry, setFocusedEntry] = useState(null)
  const [savingStatus, setSavingStatus] = useState(false)
  const actionHandledRef = useRef('')

  const selectedPatient = useMemo(
    () => patients.find((patient) => patientId(patient) === patientFilter) ?? patients[0] ?? null,
    [patientFilter, patients],
  )

  const loadPatients = useCallback(async () => {
    if (!activeProfile?.id) {
      return
    }

    setPatientsLoading(true)
    if (isElderly) {
      setPatients([{ ...activeProfile, id: activeProfile.id, profileId: activeProfile.id, source: 'self', status: 'ACTIVE' }])
      setPatientFilter(activeProfile.id)
      setPatientsLoading(false)
      return
    }

    try {
      const [profilesPage, acceptedRelationships] = await Promise.all([
        getProfiles({ page: 0, size: 100 }),
        getCaregiverRelationships(),
      ])
      setPatients(mergeCaregiverPatients(
        asPageContent(profilesPage),
        Array.isArray(acceptedRelationships) ? acceptedRelationships : [],
      ))
    } catch (error) {
      notify.apiError(error, 'Không thể tải danh sách người thân')
      setPatients([])
    } finally {
      setPatientsLoading(false)
    }
  }, [activeProfile, isElderly])

  useEffect(() => {
    loadPatients()
  }, [loadPatients])

  useEffect(() => {
    if (isElderly) {
      return
    }

    const requestedProfileId = searchParams.get('profileId')
    if (requestedProfileId) {
      setPatientFilter(requestedProfileId)
    }
    const requestedSearch = searchParams.get('q')
    if (requestedSearch) {
      setSearchText(requestedSearch)
    }
  }, [isElderly, searchParams])

  useEffect(() => {
    if (!patients.length) {
      setPatientFilter('all')
      return
    }

    if (isElderly) {
      setPatientFilter(patientId(patients[0]) ?? 'all')
      return
    }

    const ids = patients.map(patientId).filter(Boolean)
    if (patientFilter !== 'all' && !ids.includes(patientFilter)) {
      setPatientFilter('all')
    }
  }, [isElderly, patientFilter, patients])

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

  const loadMedications = useCallback(async () => {
    if (!patients.length) {
      setMedicationsByPatient({})
      return
    }

    setMedicationsLoading(true)
    try {
      const isActive = statusFilter === 'all' ? undefined : true
      const results = await Promise.allSettled(
        patients.map(async (patient) => {
          const id = patientId(patient)
          const page = await getMedications({ patientId: id, isActive, page: 0, size: 100 })
          return [id, asPageContent(page)]
        }),
      )

      const nextMap = {}
      const allMedications = []
      results.forEach((result) => {
        if (result.status === 'fulfilled') {
          const [id, medications] = result.value
          nextMap[id] = medications
          allMedications.push(...medications)
        }
      })

      setMedicationsByPatient(nextMap)
      await hydratePills(allMedications)

      if (results.every((result) => result.status === 'rejected')) {
        notify.error('Không thể tải lịch uống thuốc')
      }
    } finally {
      setMedicationsLoading(false)
    }
  }, [hydratePills, patients, statusFilter])

  useEffect(() => {
    loadMedications()
  }, [loadMedications])

  const loadDoseEvents = useCallback(async () => {
    if (!patients.length) {
      setDoseEventsByKey({})
      return
    }

    setEventsLoading(true)
    try {
      const results = await Promise.allSettled(
        patients.map(async (patient) => {
          const id = patientId(patient)
          const page = await getDoseEvents({ patientId: id, startDate: dateFrom, endDate: dateTo, page: 0, size: 1000 })
          return asPageContent(page)
        }),
      )

      const nextMap = {}
      results.forEach((result) => {
        if (result.status !== 'fulfilled') {
          return
        }
        result.value.forEach((doseEvent) => {
          nextMap[doseEventKey(doseEvent)] = doseEvent
        })
      })

      setDoseEventsByKey(nextMap)

      if (results.every((result) => result.status === 'rejected')) {
        notify.error('Không thể tải trạng thái uống thuốc')
      }
    } finally {
      setEventsLoading(false)
    }
  }, [dateFrom, dateTo, patients])

  useEffect(() => {
    loadDoseEvents()
  }, [loadDoseEvents])

  useEffect(() => {
    if (!drawerMode && !focusedEntry) {
      return undefined
    }

    document.body.classList.add('caregiver-medication-lock-scroll')
    return () => {
      document.body.classList.remove('caregiver-medication-lock-scroll')
    }
  }, [drawerMode, focusedEntry])

  const allPatientMedications = useMemo(
    () => patients.flatMap((patient) => (
      (medicationsByPatient[patientId(patient)] ?? []).map((medication) => ({ medication, patient }))
    )),
    [medicationsByPatient, patients],
  )

  const entries = useMemo(
    () => flattenScheduleEntries(patients, medicationsByPatient, dateFrom, dateTo),
    [dateFrom, dateTo, medicationsByPatient, patients],
  )

  const filteredEntries = useMemo(() => {
    const search = searchText.trim().toLowerCase()

    return entries.filter((entry) => {
      const pill = pillMap[normalizePillId(entry.medication.pillId)]
      const matchesPatient = patientFilter === 'all' || patientId(entry.patient) === patientFilter
      const matchesFrequency = frequencyFilter === 'all' || scheduleType(entry.schedule) === frequencyFilter
      return matchesPatient && matchesFrequency && matchSearch(entry, pill, search)
    })
  }, [entries, frequencyFilter, patientFilter, pillMap, searchText])

  const groupedEntries = useMemo(() => groupEntriesByDate(filteredEntries), [filteredEntries])
  const eventDoseForEntry = useCallback(
    (entry) => doseEventsByKey[entryDoseEventKey(entry)],
    [doseEventsByKey],
  )

  const unscheduledMedications = useMemo(() => {
    const search = searchText.trim().toLowerCase()
    return allPatientMedications.filter(({ medication, patient }) => {
      const schedules = medication.schedules ?? []
      const hasFixedTimes = schedules.some((schedule) => (
        scheduleType(schedule) !== 'AS_NEEDED' &&
        medicationDoseTimes(schedule).some((dose) => doseTime(dose))
      ))
      if (hasFixedTimes) {
        return false
      }
      if (patientFilter !== 'all' && patientId(patient) !== patientFilter) {
        return false
      }
      if (frequencyFilter !== 'all') {
        return false
      }
      const pill = pillMap[normalizePillId(medication.pillId)]
      return matchSearch({ medication, patient, schedule: {} }, pill, search)
    })
  }, [allPatientMedications, frequencyFilter, patientFilter, pillMap, searchText])

  const summary = useMemo(() => {
    const todayKey = localDateInput()
    const tomorrowKey = addDays(todayKey, 1)
    const statusCount = (status) => filteredEntries.filter((entry) => (
      doseEventsByKey[entryDoseEventKey(entry)]?.status === status
    )).length
    return {
      today: filteredEntries.filter((entry) => entry.dateKey === todayKey).length,
      tomorrow: filteredEntries.filter((entry) => entry.dateKey === tomorrowKey).length,
      cards: filteredEntries.length,
      missing: unscheduledMedications.length,
      patients: patients.length,
      pending: filteredEntries.filter((entry) => !doseEventsByKey[entryDoseEventKey(entry)] || doseEventsByKey[entryDoseEventKey(entry)]?.status === 'PENDING').length,
      taken: statusCount('TAKEN'),
      missed: statusCount('MISSED'),
      overdue: statusCount('OVERDUE'),
    }
  }, [doseEventsByKey, filteredEntries, patients.length, unscheduledMedications.length])

  const setQuickRange = (type) => {
    const today = localDateInput()
    if (type === 'today') {
      setDateFrom(today)
      setDateTo(today)
    } else if (type === 'tomorrow') {
      const tomorrow = addDays(today, 1)
      setDateFrom(tomorrow)
      setDateTo(tomorrow)
    } else if (type === 'month') {
      const start = dateFromInput(today)
      const end = new Date(start.getFullYear(), start.getMonth() + 1, 0)
      setDateFrom(today)
      setDateTo(localDateInput(end))
    } else {
      setDateFrom(today)
      setDateTo(addDays(today, 7))
    }
  }

  const openCreateDrawer = useCallback((patient = selectedPatient) => {
    if (!patient) {
      notify.warning('Chọn hoặc tạo hồ sơ cho người thân trước khi tạo lịch uống')
      return
    }

    setEditingMedication(null)
    setForm(emptyMedicationForm(patient))
    setDrawerMode('create')
  }, [selectedPatient])

  const openEditDrawer = useCallback(async (medication, patient) => {
    const normalizedPillId = normalizePillId(medication?.pillId)
    let pill = pillMap[normalizedPillId]

    if (!pill && canFetchPillById(normalizedPillId)) {
      try {
        pill = await getPillById(normalizedPillId)
        setPillMap((current) => ({ ...current, [normalizedPillId]: pill }))
      } catch {
        pill = null
      }
    }

    setEditingMedication(medication)
    setForm(medicationToForm(medication, patient, pill))
    setDrawerMode('edit')
  }, [pillMap])

  useEffect(() => {
    const requestedAction = searchParams.get('action')
    const requestedProfileId = searchParams.get('profileId')
    const actionKey = `${requestedAction ?? ''}:${requestedProfileId ?? ''}:${patients.length}`
    if (requestedAction === 'add' && patients.length && actionHandledRef.current !== actionKey) {
      actionHandledRef.current = actionKey
      const requestedPatient = patients.find((patient) => patientId(patient) === requestedProfileId) ?? patients[0]
      openCreateDrawer(requestedPatient)
    }
  }, [openCreateDrawer, patients, searchParams])

  const submitMedication = async (event) => {
    event.preventDefault()

    const validationMessage = validateMedicationForm(form)
    if (validationMessage) {
      notify.warning(validationMessage)
      return
    }

    setSaving(true)
    try {
      const payload = buildMedicationPayload(form)
      if (drawerMode === 'edit' && editingMedication?.id) {
        const { patientId: _patientId, ...updatePayload } = payload
        await updateCaregiverMedication(editingMedication.id, updatePayload)
        notify.success('Đã cập nhật lịch uống')
      } else {
        await createCaregiverMedication(payload)
        notify.success('Đã tạo thuốc và lịch uống')
      }

      setDrawerMode(null)
      setEditingMedication(null)
      await loadMedications()
    } catch (error) {
      notify.error(drawerMode === 'edit' ? 'Không thể cập nhật lịch uống' : 'Không thể tạo lịch uống', {
        description: getApiErrorMessage(error),
      })
    } finally {
      setSaving(false)
    }
  }

  const confirmDeleteMedication = async () => {
    if (!deleteTarget?.medication?.id) {
      return
    }

    try {
      await deleteCaregiverMedication(deleteTarget.medication.id)
      notify.success('Đã xóa thuốc và lịch liên quan')
      setDeleteTarget(null)
      await loadMedications()
    } catch (error) {
      notify.apiError(error, 'Không thể xóa thuốc')
    }
  }

  const handleDoseStatusChange = async (status) => {
    const currentEvent = focusedEntry ? eventDoseForEntry(focusedEntry) : null
    if (!currentEvent?.id) {
      notify.warning('Cữ thuốc này chưa thể cập nhật trạng thái')
      return
    }

    setSavingStatus(true)
    try {
      const payload = {
        status,
        note: 'Elderly tự cập nhật từ trang lịch uống',
      }

      if (status === 'TAKEN' || status === 'OVERDUE') {
        payload.takenAt = localDateTimePayload()
      }

      const updatedEvent = await updateElderlyDoseStatus(currentEvent.id, payload)
      setDoseEventsByKey((current) => ({
        ...current,
        [doseEventKey(updatedEvent)]: updatedEvent,
      }))
      notify.success('Đã cập nhật tình trạng uống thuốc')
    } catch (error) {
      notify.apiError(error, 'Không thể cập nhật tình trạng uống thuốc')
    } finally {
      setSavingStatus(false)
    }
  }

  const focusedDoseEvent = focusedEntry ? eventDoseForEntry(focusedEntry) : null
  const loading = patientsLoading || medicationsLoading || eventsLoading
  const canCreate = !isElderly && Boolean(patients.length)
  const summaryCards = isElderly
    ? [
      { icon: CalendarClock, label: 'Cữ hôm nay', value: summary.today },
      { icon: CheckCircle2, label: 'Đã uống', tone: 'emerald', value: summary.taken },
      { icon: CircleAlert, label: 'Chưa uống', tone: summary.missed ? 'amber' : 'slate', value: summary.missed },
      { icon: Clock3, label: 'Uống muộn', tone: summary.overdue ? 'amber' : 'slate', value: summary.overdue },
      { icon: CalendarDays, label: 'Trong bộ lọc', tone: 'blue', value: summary.cards },
    ]
    : [
      { icon: UserRound, label: 'Elderly đang chăm sóc', tone: 'slate', value: summary.patients },
      { icon: CalendarClock, label: 'Hôm nay', value: summary.today },
      { icon: CalendarDays, label: 'Ngày mai', tone: 'blue', value: summary.tomorrow },
      { icon: CheckCircle2, label: 'Thẻ trong bộ lọc', tone: 'emerald', value: summary.cards },
      { icon: PackageCheck, label: 'Chưa có lịch', tone: summary.missing ? 'amber' : 'emerald', value: summary.missing },
    ]

  return (
    <div className="caregiver-schedule-page mx-auto grid w-full max-w-[1500px] gap-4 pb-4 text-slate-950 sm:gap-5 lg:pb-6 xl:gap-6">
      <section className="schedule-hero">
        <div className="min-w-0">
          <p>Lịch uống thuốc</p>
          <h1>{isElderly ? 'Lịch uống thuốc của tôi' : 'Điều phối lịch uống thuốc cho người thân'}</h1>
          <span>
            {isElderly
              ? 'Xem các cữ thuốc trong ngày, mở từng thẻ để xác nhận đã uống, chưa uống hoặc uống muộn.'
              : 'Tổng hợp mọi lịch thuốc cho người thân của bạn, giúp quản lý và theo dõi việc dùng thuốc dễ dàng hơn.'}
          </span>
        </div>
        <div className="schedule-hero__actions">
          <Button disabled={loading} variant="ghost" onClick={() => {
            loadMedications()
            loadDoseEvents()
          }}>
            {loading ? <Loader2 className="animate-spin" size={17} /> : <RefreshCcw size={17} />}
            Làm mới
          </Button>
          {!isElderly ? (
            <Button disabled={!canCreate} variant="primary" onClick={() => openCreateDrawer()}>
              <Plus size={17} />
              Tạo thuốc và lịch
            </Button>
          ) : null}
        </div>
      </section>

      <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        {summaryCards.map((card) => (
          <SummaryCard
            icon={card.icon}
            key={card.label}
            label={card.label}
            tone={card.tone}
            value={card.value}
          />
        ))}
      </section>

      <section className="schedule-filter-panel">
        <div className="schedule-filter-panel__title">
          <Filter size={18} />
          <strong>Bộ lọc lịch</strong>
        </div>

        <div className="grid gap-3 lg:grid-cols-[minmax(170px,0.8fr)_minmax(170px,0.8fr)_minmax(200px,1fr)_minmax(170px,0.75fr)_minmax(150px,0.65fr)]">
          <label className="schedule-filter-field">
            Từ ngày
            <input className={fieldClass} type="date" value={dateFrom} onChange={(event) => setDateFrom(event.target.value)} />
          </label>
          <label className="schedule-filter-field">
            Đến ngày
            <input className={fieldClass} type="date" value={dateTo} onChange={(event) => setDateTo(event.target.value)} />
          </label>
          <label className="schedule-filter-field">
            Người thân
            <select className={fieldClass} value={patientFilter} onChange={(event) => setPatientFilter(event.target.value)}>
              <option value="all">Tất cả người thân</option>
              {patients.map((patient) => (
                <option key={patientId(patient)} value={patientId(patient)}>
                  {fullName(patient)}
                </option>
              ))}
            </select>
          </label>
          <label className="schedule-filter-field">
            Tần suất
            <select className={fieldClass} value={frequencyFilter} onChange={(event) => setFrequencyFilter(event.target.value)}>
              <option value="all">Tất cả lịch</option>
              {SCHEDULE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </label>
          <label className="schedule-filter-field">
            Trạng thái thuốc
            <select className={fieldClass} value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="active">Đang dùng</option>
              <option value="all">Tất cả</option>
            </select>
          </label>
        </div>

        <div className="schedule-filter-panel__bottom">
          <div className="schedule-search-field">
            <Search size={18} />
            <input
              placeholder="Tìm theo tên thuốc, người thân, hoạt chất, bác sĩ..."
              value={searchText}
              onChange={(event) => setSearchText(event.target.value)}
            />
          </div>
          <div className="schedule-quick-filters">
            {[
              { value: 'today', label: 'Hôm nay' },
              { value: 'tomorrow', label: 'Ngày mai' },
              { value: 'week', label: '7 ngày' },
              { value: 'month', label: 'Tháng này' },
            ].map((item) => (
              <button key={item.value} type="button" onClick={() => setQuickRange(item.value)}>
                {item.label}
              </button>
            ))}
          </div>
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
                <span>{group.entries.length} thẻ lịch</span>
              </div>

              <div className="grid gap-4 xl:grid-cols-2">
                {group.entries.map((entry) => (
                  <ScheduleCard
                    entry={entry}
                    eventDose={eventDoseForEntry(entry)}
                    isElderly={isElderly}
                    key={entry.key}
                    pill={pillMap[normalizePillId(entry.medication.pillId)]}
                    onDelete={(medication, patient) => setDeleteTarget({ medication, patient })}
                    onEdit={openEditDrawer}
                    onFocus={setFocusedEntry}
                  />
                ))}
              </div>
            </section>
          ))
        ) : (
          <EmptyScheduleState canCreate={canCreate} isElderly={isElderly} loading={loading} onCreate={() => openCreateDrawer()} />
        )}
      </section>

      {!isElderly ? (
        <>
          <UnscheduledMedications
            items={unscheduledMedications}
            pillMap={pillMap}
            onCreate={() => openCreateDrawer()}
            onDelete={(medication, patient) => setDeleteTarget({ medication, patient })}
            onEdit={openEditDrawer}
          />

          <MedicationFormModal
            activeRole="CAREGIVER"
            form={form}
            mode={drawerMode}
            patients={patients}
            saving={saving}
            selectedMedication={editingMedication}
            setForm={setForm}
            onClose={() => {
              setDrawerMode(null)
              setEditingMedication(null)
            }}
            onSubmit={submitMedication}
          />

          <ConfirmDialog
            confirmLabel="Xóa thuốc"
            description={
              deleteTarget
                ? `${deleteTarget.medication.nickname || 'Thuốc này'} của ${fullName(deleteTarget.patient)} sẽ bị xóa cùng các lịch uống liên quan.`
                : ''
            }
            open={Boolean(deleteTarget)}
            title="Xóa thuốc khỏi lịch chăm sóc?"
            onConfirm={confirmDeleteMedication}
            onOpenChange={(open) => {
              if (!open) {
                setDeleteTarget(null)
              }
            }}
          />
        </>
      ) : null}

      <ElderlyDoseFocusModal
        entry={focusedEntry}
        eventDose={focusedDoseEvent}
        pill={focusedEntry ? pillMap[normalizePillId(focusedEntry.medication.pillId)] : null}
        savingStatus={savingStatus}
        onClose={() => setFocusedEntry(null)}
        onStatusChange={handleDoseStatusChange}
      />
    </div>
  )
}

export default SchedulePage
