import {
  AlertTriangle,
  CalendarClock,
  Camera,
  CheckCircle2,
  Clock3,
  FilePenLine,
  Loader2,
  PackageCheck,
  Pill,
  Plus,
  RefreshCcw,
  Search,
  Trash2,
  UploadCloud,
  X,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'

import { Button } from '../components/ui/Button.jsx'
import { CameraCapture } from '../components/ui/CameraCapture.jsx'
import { ConfirmDialog } from '../components/ui/Modal.jsx'
import { getApiErrorMessage } from '../lib/apiClient.js'
import { notify } from '../lib/toast.js'
import { useAuthStore } from '../modules/auth/authStore.js'
import {
  getCaregiverRelationships,
  getProfiles,
} from '../modules/profile/profileApi.js'
import {
  analyzeMedicationImage,
  asPageContent,
  createCaregiverMedication,
  deleteCaregiverMedication,
  getMedications,
  getPillById,
  normalizePillId,
  searchPills,
  updateCaregiverMedication,
} from '../modules/medication/medicationApi.js'
import '../styles/caregiver/medications.css'

const MEAL_OPTIONS = [
  { value: 'BEFORE_MEAL', label: 'Trước bữa ăn' },
  { value: 'AFTER_MEAL', label: 'Sau bữa ăn' },
  { value: 'WITH_MEAL', label: 'Trong bữa ăn' },
  { value: 'ANYTIME', label: 'Bất kỳ lúc nào' },
  { value: 'BEFORE_SLEEP', label: 'Trước khi ngủ' },
]

const MEAL_LABELS = MEAL_OPTIONS.reduce((labels, option) => {
  labels[option.value] = option.label
  return labels
}, {})

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

const WEEKDAY_OPTIONS = [
  { value: 'MON', label: 'T2' },
  { value: 'TUE', label: 'T3' },
  { value: 'WED', label: 'T4' },
  { value: 'THU', label: 'T5' },
  { value: 'FRI', label: 'T6' },
  { value: 'SAT', label: 'T7' },
  { value: 'SUN', label: 'CN' },
]

const UNIT_OPTIONS = ['Viên', 'ml', 'mg', 'gói', 'ống', 'giọt', 'lần']
const ROUTE_OPTIONS = ['Uống', 'Ngậm', 'Bôi', 'Tiêm', 'Nhỏ mắt', 'Xịt']

const fieldClass =
  'min-h-11 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-bold text-slate-950 outline-none transition focus:border-emerald-400 focus:ring-4 focus:ring-emerald-100 disabled:bg-slate-50 disabled:text-slate-400'
const labelClass = 'grid gap-2 text-sm font-black text-slate-600'

function cx(...classes) {
  return classes.filter(Boolean).join(' ')
}

function localDateInput() {
  const date = new Date()
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset())
  return date.toISOString().slice(0, 10)
}

function localId(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function fullName(profile) {
  return [profile?.firstName, profile?.lastName].filter(Boolean).join(' ').trim() || 'Hồ sơ PharmAgent'
}

function patientId(profile) {
  return profile?.profileId ?? profile?.id
}

function patientInitials(profile) {
  const first = profile?.firstName?.trim()?.[0] ?? ''
  const last = profile?.lastName?.trim()?.[0] ?? ''
  return `${first}${last}`.toUpperCase() || 'PA'
}

function primaryPillImage(pill) {
  const images = pill?.images ?? []
  return (
    images.find((image) => image.isPrimary)?.imageUrl ||
    images[0]?.imageUrl ||
    pill?.imageUrls?.[0] ||
    pill?.primary_image_url ||
    pill?.primaryImageUrl ||
    ''
  )
}

function pillName(pill, fallback = 'Chưa có tên thuốc') {
  return pill?.name || pill?.display_name || pill?.title || fallback
}

function medicationDoseTimes(schedule) {
  return schedule?.medDoses ?? schedule?.times ?? schedule?.scheduleTimeList ?? []
}

function timeValue(dose) {
  return dose?.takenTime ?? dose?.timeOfDay ?? ''
}

function quantityValue(dose) {
  return dose?.quantity ?? dose?.doseAmount ?? ''
}

function normalizeTimeInput(value) {
  if (!value) {
    return ''
  }
  return String(value).slice(0, 5)
}

function toApiTime(value) {
  const time = normalizeTimeInput(value)
  return time.length === 5 ? `${time}:00` : time
}

function optionalText(value) {
  const trimmed = typeof value === 'string' ? value.trim() : ''
  return trimmed || null
}

function newSchedule(startDate = localDateInput(), doseAmount = '1') {
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
    times: [newDoseTime('08:00', doseAmount)],
  }
}

function newDoseTime(timeOfDay = '08:00', doseAmount = '1') {
  return {
    localId: localId('dose'),
    timeOfDay,
    doseAmount,
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
    schedules: [newSchedule(startDate, '1')],
  }
}

function medicationToForm(medication, patientProfile, pill) {
  const startDate = medication?.startDate ?? localDateInput()
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
      timeOfDay: normalizeTimeInput(timeValue(dose)),
      doseAmount: String(quantityValue(dose) || medication?.dosageAmount || '1'),
    })),
  }))

  return {
    patientId: medication?.patientId ?? patientId(patientProfile) ?? '',
    pillId: normalizePillId(medication?.pillId),
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
    schedules: schedules.length ? schedules : [newSchedule(startDate, String(medication?.dosageAmount ?? '1'))],
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
      frequencyType: schedule.frequencyType,
      interval: Number.parseInt(schedule.interval, 10) || 1,
      daysOfWeek: schedule.frequencyType === 'WEEKLY' ? schedule.daysOfWeek : [],
      reminderEnabled: Boolean(schedule.reminderEnabled),
      reminderMinutesBefore: Number.parseInt(schedule.reminderMinutesBefore, 10) || 0,
      note: optionalText(schedule.note),
      startDate: schedule.startDate || form.startDate,
      endDate: schedule.endDate || form.endDate || null,
      isActive: true,
      times: schedule.times.map((dose) => ({
        id: dose.id,
        timeOfDay: toApiTime(dose.timeOfDay),
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
    return schedule.frequencyType !== 'AS_NEEDED' && schedule.times.length === 0
  })

  if (invalidSchedule) {
    return 'Kiểm tra lại ngày và khung giờ trong lịch uống'
  }

  return ''
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

function PatientAvatar({ profile, size = 'md' }) {
  const sizeClass = size === 'lg' ? 'h-14 w-14 text-base' : 'h-11 w-11 text-sm'
  return (
    <span className={cx('grid shrink-0 place-items-center overflow-hidden rounded-lg bg-emerald-100 font-black text-emerald-800', sizeClass)}>
      {profile?.avatarUrl ? <img alt="" className="h-full w-full object-cover" src={profile.avatarUrl} /> : patientInitials(profile)}
    </span>
  )
}

function PillThumb({ pill }) {
  const imageUrl = primaryPillImage(pill)
  return (
    <span className="grid h-12 w-12 shrink-0 place-items-center overflow-hidden rounded-lg border border-slate-200 bg-white text-emerald-700">
      {imageUrl ? <img alt="" className="h-full w-full object-cover" src={imageUrl} /> : <Pill size={22} />}
    </span>
  )
}

function SummaryTile({ icon: Icon, label, value, tone = 'emerald' }) {
  const toneClass = {
    amber: 'bg-amber-50 text-amber-800',
    blue: 'bg-sky-50 text-sky-800',
    rose: 'bg-rose-50 text-rose-800',
    emerald: 'bg-emerald-50 text-emerald-800',
  }[tone]

  return (
    <article className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm shadow-slate-100">
      <div className={cx('grid h-10 w-10 place-items-center rounded-lg', toneClass)}>
        <Icon size={20} />
      </div>
      <span className="mt-4 block text-xs font-black uppercase text-slate-500">{label}</span>
      <strong className="mt-1 block text-2xl font-black leading-none text-slate-950">{value}</strong>
    </article>
  )
}

function scheduleLabel(schedule) {
  const type = schedule?.scheduleType ?? schedule?.frequencyType ?? 'DAILY'
  const interval = schedule?.frequencyInterval ?? schedule?.interval ?? 1
  if (type === 'WEEKLY') {
    const days = schedule?.daysOfWeek?.length ? schedule.daysOfWeek.join(', ') : 'theo ngày bắt đầu'
    return `${SCHEDULE_LABELS[type] ?? type} · ${days}`
  }
  if (type === 'INTERVAL') {
    return `Mỗi ${interval} ngày`
  }
  return SCHEDULE_LABELS[type] ?? type
}

function MedicationRow({ medication, pill, active, onClick }) {
  const schedules = medication.schedules ?? []
  const timesCount = schedules.reduce((count, schedule) => count + medicationDoseTimes(schedule).length, 0)
  const lowStock = Number(medication.totalQuantity ?? 0) <= 7

  return (
    <button
      className={cx(
        'caregiver-medication-row grid w-full gap-3 rounded-lg border p-3 text-left transition hover:border-emerald-200 hover:bg-emerald-50/50',
        active ? 'border-emerald-300 bg-emerald-50 shadow-sm' : 'border-slate-200 bg-white',
      )}
      type="button"
      onClick={onClick}
    >
      <div className="flex min-w-0 items-start gap-3">
        <PillThumb pill={pill} />
        <div className="min-w-0 flex-1">
          <strong className="block truncate text-base font-black text-slate-950">
            {medication.nickname || pillName(pill, 'Thuốc chưa đặt tên')}
          </strong>
          <span className="mt-1 block truncate text-sm font-bold text-slate-500">
            {pillName(pill, medication.pillId)}
          </span>
        </div>
        {lowStock ? (
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-amber-50 text-amber-700" title="Sắp hết thuốc">
            <AlertTriangle size={17} />
          </span>
        ) : null}
      </div>
      <div className="flex flex-wrap gap-2 text-xs font-black text-slate-600">
        <span className="rounded-full bg-slate-100 px-3 py-1">{medication.dosageAmount} {medication.dosageUnit}</span>
        <span className="rounded-full bg-sky-50 px-3 py-1 text-sky-700">{timesCount} khung giờ</span>
        <span className="rounded-full bg-emerald-50 px-3 py-1 text-emerald-700">Còn {medication.totalQuantity ?? 0}</span>
      </div>
    </button>
  )
}

function MedicationDetail({ medication, patient, pill, onEdit, onDelete }) {
  if (!medication) {
    return (
      <article className="grid min-h-[420px] content-center justify-items-center rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
        <Pill className="text-slate-300" size={44} />
        <h2 className="mt-4 text-xl font-black text-slate-950">Chưa chọn thuốc</h2>
        <p className="mt-2 max-w-md text-sm font-bold text-slate-500">
          Danh sách bên trái hiển thị thuốc theo từng hồ sơ người thân.
        </p>
      </article>
    )
  }

  const schedules = medication.schedules ?? []
  const totalTimes = schedules.reduce((count, schedule) => count + medicationDoseTimes(schedule).length, 0)

  return (
    <article className="caregiver-medication-detail rounded-lg border border-slate-200 bg-white p-4 shadow-sm shadow-slate-100 lg:p-5">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-100 pb-4">
        <div className="flex min-w-0 items-start gap-3">
          <PillThumb pill={pill} />
          <div className="min-w-0">
            <p className="text-xs font-black uppercase text-emerald-700">{fullName(patient)}</p>
            <h2 className="mt-1 break-words text-2xl font-black leading-tight text-slate-950">
              {medication.nickname || pillName(pill, 'Thuốc chưa đặt tên')}
            </h2>
            <p className="mt-1 text-sm font-bold text-slate-500">{pillName(pill, medication.pillId)}</p>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button size="sm" variant="ghost" onClick={onEdit}>
            <FilePenLine size={16} />
            Sửa
          </Button>
          <Button size="sm" variant="danger" onClick={onDelete}>
            <Trash2 size={16} />
            Xóa
          </Button>
        </div>
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <InfoChip label="Liều dùng" value={`${medication.dosageAmount} ${medication.dosageUnit}`} />
        <InfoChip label="Cách dùng" value={medication.route || 'Chưa cập nhật'} />
        <InfoChip label="Bữa ăn" value={MEAL_LABELS[medication.mealRelation] ?? medication.mealRelation} />
        <InfoChip label="Kho thuốc" value={`${medication.totalQuantity ?? 0} còn lại`} />
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
        <section className="rounded-lg bg-slate-50 p-4">
          <h3 className="text-sm font-black uppercase text-slate-500">Kê đơn</h3>
          <dl className="mt-3 grid gap-3 text-sm">
            <DetailRow label="Mục đích" value={medication.purpose || 'Chưa cập nhật'} />
            <DetailRow label="Người kê đơn" value={medication.prescribedBy || 'Chưa cập nhật'} />
            <DetailRow label="Ngày bắt đầu" value={medication.startDate || 'Chưa cập nhật'} />
            <DetailRow label="Ngày kết thúc" value={medication.endDate || 'Không đặt'} />
            <DetailRow label="Ghi chú" value={medication.instruction || 'Không có ghi chú'} />
          </dl>
        </section>

        <section className="rounded-lg bg-emerald-50/70 p-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h3 className="text-sm font-black uppercase text-emerald-800">Lịch nhắc</h3>
            <span className="rounded-full bg-white px-3 py-1 text-xs font-black text-emerald-800">
              {totalTimes} khung giờ
            </span>
          </div>
          <div className="mt-3 grid gap-3">
            {schedules.length ? (
              schedules.map((schedule) => (
                <div className="rounded-lg border border-emerald-100 bg-white p-3" key={schedule.id ?? scheduleLabel(schedule)}>
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <strong className="text-sm font-black text-slate-950">{scheduleLabel(schedule)}</strong>
                    <span className="text-xs font-black text-slate-500">
                      {schedule.startDate || medication.startDate} → {schedule.endDate || medication.endDate || 'Không đặt'}
                    </span>
                  </div>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {medicationDoseTimes(schedule).map((dose) => (
                      <span
                        className="inline-flex items-center gap-2 rounded-full bg-slate-100 px-3 py-1 text-xs font-black text-slate-700"
                        key={dose.id ?? `${timeValue(dose)}-${quantityValue(dose)}`}
                      >
                        <Clock3 size={13} />
                        {normalizeTimeInput(timeValue(dose))} · {quantityValue(dose)} {medication.dosageUnit}
                      </span>
                    ))}
                  </div>
                </div>
              ))
            ) : (
              <p className="rounded-lg border border-dashed border-emerald-200 bg-white p-4 text-sm font-bold text-slate-500">
                Chưa có lịch nhắc cho thuốc này.
              </p>
            )}
          </div>
        </section>
      </div>
    </article>
  )
}

function InfoChip({ label, value }) {
  return (
    <div className="rounded-lg border border-slate-100 bg-slate-50 p-3">
      <span className="text-xs font-black uppercase text-slate-500">{label}</span>
      <strong className="mt-1 block break-words text-sm font-black text-slate-950">{value}</strong>
    </div>
  )
}

function DetailRow({ label, value }) {
  return (
    <div className="grid gap-1">
      <dt className="font-black text-slate-500">{label}</dt>
      <dd className="m-0 break-words font-bold text-slate-900">{value}</dd>
    </div>
  )
}

export function MedicationsPage() {
  const [searchParams] = useSearchParams()
  const activeProfile = useAuthStore((state) => state.activeProfile)
  const activeRole = activeProfile?.role
  const [patients, setPatients] = useState([])
  const [patientsLoading, setPatientsLoading] = useState(true)
  const [selectedPatientId, setSelectedPatientId] = useState('')
  const [medications, setMedications] = useState([])
  const [medicationsLoading, setMedicationsLoading] = useState(false)
  const [activeFilter, setActiveFilter] = useState('active')
  const [selectedMedicationId, setSelectedMedicationId] = useState('')
  const [pillMap, setPillMap] = useState({})
  const [drawerMode, setDrawerMode] = useState(null)
  const [form, setForm] = useState(() => emptyMedicationForm())
  const [saving, setSaving] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const actionHandledRef = useRef('')

  const requestedProfileId = searchParams.get('profileId')
  const requestedAction = searchParams.get('action')
  const searchText = searchParams.get('q')?.trim()?.toLowerCase() ?? ''

  const selectedPatient = useMemo(
    () => patients.find((profile) => patientId(profile) === selectedPatientId) ?? null,
    [patients, selectedPatientId],
  )

  const selectedMedication = useMemo(
    () => medications.find((medication) => medication.id === selectedMedicationId) ?? null,
    [medications, selectedMedicationId],
  )

  const loadPatients = useCallback(async () => {
    if (!activeProfile?.id) {
      return
    }

    setPatientsLoading(true)
    try {
      if (activeRole === 'CAREGIVER') {
        const [profilesPage, acceptedRelationships] = await Promise.all([
          getProfiles({ page: 0, size: 100 }),
          getCaregiverRelationships(),
        ])
        const nextPatients = mergeCaregiverPatients(
          asPageContent(profilesPage),
          Array.isArray(acceptedRelationships) ? acceptedRelationships : [],
        )
        setPatients(nextPatients)
      } else {
        setPatients([{ ...activeProfile, profileId: activeProfile.id, source: 'self', status: 'ACTIVE' }])
      }
    } catch (error) {
      notify.apiError(error, 'Không thể tải hồ sơ dùng thuốc')
      setPatients([])
    } finally {
      setPatientsLoading(false)
    }
  }, [activeProfile, activeRole])

  useEffect(() => {
    loadPatients()
  }, [loadPatients])

  useEffect(() => {
    if (!patients.length) {
      setSelectedPatientId('')
      return
    }

    const patientIds = patients.map((profile) => patientId(profile)).filter(Boolean)
    const requestedPatientIsAvailable = requestedProfileId && patientIds.includes(requestedProfileId)
    const currentPatientIsAvailable = selectedPatientId && patientIds.includes(selectedPatientId)
    const nextPatientId = requestedPatientIsAvailable
      ? requestedProfileId
      : currentPatientIsAvailable
        ? selectedPatientId
        : patientIds[0]

    if (nextPatientId !== selectedPatientId) {
      setSelectedPatientId(nextPatientId)
    }
  }, [patients, requestedProfileId, selectedPatientId])

  const hydratePills = useCallback(async (items) => {
    const ids = [...new Set(items.map((item) => normalizePillId(item.pillId)).filter(Boolean))]

    if (!ids.length) {
      return
    }

    const results = await Promise.allSettled(ids.map((id) => getPillById(id)))
    const nextMap = {}
    results.forEach((result) => {
      if (result.status === 'fulfilled' && result.value?.id) {
        nextMap[normalizePillId(result.value.id)] = result.value
      }
    })
    if (Object.keys(nextMap).length) {
      setPillMap((current) => ({ ...current, ...nextMap }))
    }
  }, [])

  const loadMedicationsForPatient = useCallback(async () => {
    if (!selectedPatientId) {
      setMedications([])
      return
    }

    setMedicationsLoading(true)
    try {
      const isActive = activeFilter === 'all' ? undefined : activeFilter === 'active'
      const page = await getMedications({ patientId: selectedPatientId, isActive, page: 0, size: 100 })
      const nextMedications = asPageContent(page)
      setMedications(nextMedications)
      await hydratePills(nextMedications)
      setSelectedMedicationId((current) => {
        if (nextMedications.some((medication) => medication.id === current)) {
          return current
        }
        return nextMedications[0]?.id ?? ''
      })
    } catch (error) {
      notify.apiError(error, 'Không thể tải danh sách thuốc')
      setMedications([])
    } finally {
      setMedicationsLoading(false)
    }
  }, [activeFilter, hydratePills, selectedPatientId])

  useEffect(() => {
    loadMedicationsForPatient()
  }, [loadMedicationsForPatient])

  const openCreateDrawer = useCallback(() => {
    setForm(emptyMedicationForm(selectedPatient))
    setDrawerMode('create')
  }, [selectedPatient])

  const openEditDrawer = useCallback(async (medication) => {
    const normalizedPillId = normalizePillId(medication.pillId)
    let pill = pillMap[normalizedPillId]

    if (!pill && normalizedPillId) {
      try {
        pill = await getPillById(normalizedPillId)
        setPillMap((current) => ({ ...current, [normalizedPillId]: pill }))
      } catch {
        pill = null
      }
    }

    setForm(medicationToForm(medication, selectedPatient, pill))
    setDrawerMode('edit')
  }, [pillMap, selectedPatient])

  useEffect(() => {
    const actionKey = `${requestedProfileId ?? ''}:${requestedAction ?? ''}:${selectedPatientId}`
    if (requestedAction === 'add' && selectedPatientId && actionHandledRef.current !== actionKey) {
      actionHandledRef.current = actionKey
      openCreateDrawer()
    }
  }, [openCreateDrawer, requestedAction, requestedProfileId, selectedPatientId])

  const filteredMedications = useMemo(() => {
    if (!searchText) {
      return medications
    }

    return medications.filter((medication) => {
      const pill = pillMap[normalizePillId(medication.pillId)]
      return [
        medication.nickname,
        medication.purpose,
        medication.prescribedBy,
        pillName(pill, ''),
        pill?.activeIngredient,
      ]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(searchText))
    })
  }, [medications, pillMap, searchText])

  const summary = useMemo(() => {
    const activeCount = medications.filter((medication) => medication.isActive !== false).length
    const lowStock = medications.filter((medication) => Number(medication.totalQuantity ?? 0) <= 7).length
    const scheduleTimes = medications.reduce(
      (total, medication) =>
        total + (medication.schedules ?? []).reduce((count, schedule) => count + medicationDoseTimes(schedule).length, 0),
      0,
    )
    return { activeCount, lowStock, scheduleTimes }
  }, [medications])

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
      let saved
      if (drawerMode === 'edit' && selectedMedication?.id) {
        const { patientId: _patientId, ...updatePayload } = payload
        saved = await updateCaregiverMedication(selectedMedication.id, updatePayload)
        notify.success('Đã cập nhật thuốc')
      } else {
        saved = await createCaregiverMedication(payload)
        notify.success('Đã thêm thuốc')
      }

      setDrawerMode(null)
      await loadMedicationsForPatient()
      if (saved?.id) {
        setSelectedMedicationId(saved.id)
      }
    } catch (error) {
      notify.error(drawerMode === 'edit' ? 'Không thể cập nhật thuốc' : 'Không thể thêm thuốc', {
        description: getApiErrorMessage(error),
      })
    } finally {
      setSaving(false)
    }
  }

  const confirmDeleteMedication = async () => {
    if (!deleteTarget?.id) {
      return
    }

    try {
      await deleteCaregiverMedication(deleteTarget.id)
      notify.success('Đã xóa thuốc')
      setDeleteTarget(null)
      await loadMedicationsForPatient()
    } catch (error) {
      notify.apiError(error, 'Không thể xóa thuốc')
    }
  }

  const selectedPill = selectedMedication ? pillMap[normalizePillId(selectedMedication.pillId)] : null

  return (
    <div className="caregiver-medications-page mx-auto grid w-full max-w-[1480px] gap-4 sm:gap-5">
      <section className="caregiver-medication-hero rounded-lg border border-emerald-100 bg-gradient-to-br from-white via-emerald-50 to-sky-50 p-4 shadow-lg shadow-slate-200/60 sm:p-5 lg:p-7">
        <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(280px,0.34fr)] lg:items-end">
          <div>
            <p className="text-xs font-black uppercase text-emerald-700">
              {activeRole === 'CAREGIVER' ? 'Quản lý thuốc người thân' : 'Thuốc của tôi'}
            </p>
            <h1 className="mt-2 max-w-4xl text-3xl font-black leading-tight tracking-normal text-slate-950 sm:text-4xl">
              Hồ sơ thuốc theo từng bệnh nhân
            </h1>
            <div className="mt-4 flex flex-wrap items-center gap-3">
              <PatientAvatar profile={selectedPatient} />
              <label className="min-w-[260px] flex-1 text-sm font-black text-slate-600">
                <span className="sr-only">Chọn hồ sơ</span>
                <select
                  className={fieldClass}
                  disabled={patientsLoading || !patients.length}
                  value={selectedPatientId}
                  onChange={(event) => setSelectedPatientId(event.target.value)}
                >
                  {patients.length ? (
                    patients.map((profile) => (
                      <option key={patientId(profile)} value={patientId(profile)}>
                        {fullName(profile)}
                      </option>
                    ))
                  ) : (
                    <option value="">Chưa có hồ sơ người thân</option>
                  )}
                </select>
              </label>
            </div>
          </div>

          <div className="flex flex-wrap justify-start gap-2 lg:justify-end">
            <Button disabled={!selectedPatientId} variant="ghost" onClick={loadMedicationsForPatient}>
              <RefreshCcw size={17} />
              Làm mới
            </Button>
            <Button disabled={!selectedPatientId} variant="primary" onClick={openCreateDrawer}>
              <Plus size={17} />
              Thêm thuốc
            </Button>
          </div>
        </div>
      </section>

      <section className="grid gap-3 sm:grid-cols-3">
        <SummaryTile icon={Pill} label="Thuốc đang dùng" value={summary.activeCount} />
        <SummaryTile icon={CalendarClock} label="Khung giờ nhắc" tone="blue" value={summary.scheduleTimes} />
        <SummaryTile icon={PackageCheck} label="Sắp hết thuốc" tone={summary.lowStock ? 'amber' : 'emerald'} value={summary.lowStock} />
      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(340px,0.36fr)_minmax(0,1fr)]">
        <aside className="rounded-lg border border-slate-200 bg-white p-3 shadow-sm shadow-slate-100 sm:p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-black text-slate-950">Danh sách thuốc</h2>
            <div className="flex rounded-lg bg-slate-100 p-1">
              {[
                { value: 'active', label: 'Đang dùng' },
                { value: 'inactive', label: 'Ngưng' },
                { value: 'all', label: 'Tất cả' },
              ].map((filter) => (
                <button
                  className={cx(
                    'rounded-md px-3 py-2 text-xs font-black transition',
                    activeFilter === filter.value ? 'bg-white text-emerald-800 shadow-sm' : 'text-slate-500 hover:text-slate-900',
                  )}
                  key={filter.value}
                  type="button"
                  onClick={() => setActiveFilter(filter.value)}
                >
                  {filter.label}
                </button>
              ))}
            </div>
          </div>

          {searchText ? (
            <p className="mt-3 rounded-lg bg-sky-50 px-3 py-2 text-sm font-bold text-sky-800">
              Đang lọc theo “{searchParams.get('q')}”
            </p>
          ) : null}

          <div className="caregiver-medication-list mt-4 grid max-h-[660px] gap-3 overflow-y-auto pr-1">
            {medicationsLoading ? (
              <div className="grid min-h-[220px] place-items-center rounded-lg border border-dashed border-slate-200 text-sm font-black text-slate-500">
                <Loader2 className="animate-spin" size={24} />
              </div>
            ) : filteredMedications.length ? (
              filteredMedications.map((medication) => (
                <MedicationRow
                  active={medication.id === selectedMedicationId}
                  key={medication.id}
                  medication={medication}
                  pill={pillMap[normalizePillId(medication.pillId)]}
                  onClick={() => setSelectedMedicationId(medication.id)}
                />
              ))
            ) : (
              <div className="grid min-h-[260px] content-center justify-items-center rounded-lg border border-dashed border-slate-300 bg-slate-50 p-6 text-center">
                <Pill className="text-slate-300" size={40} />
                <h3 className="mt-3 text-lg font-black text-slate-950">Chưa có thuốc</h3>
                <p className="mt-1 text-sm font-bold text-slate-500">Thêm thuốc đầu tiên cho hồ sơ đang chọn.</p>
              </div>
            )}
          </div>
        </aside>

        <MedicationDetail
          medication={selectedMedication}
          patient={selectedPatient}
          pill={selectedPill}
          onDelete={() => setDeleteTarget(selectedMedication)}
          onEdit={() => openEditDrawer(selectedMedication)}
        />
      </section>

      <MedicationFormDrawer
        activeRole={activeRole}
        form={form}
        mode={drawerMode}
        patients={patients}
        saving={saving}
        selectedMedication={selectedMedication}
        setForm={setForm}
        onClose={() => setDrawerMode(null)}
        onSubmit={submitMedication}
      />

      <ConfirmDialog
        confirmLabel="Xóa thuốc"
        description={deleteTarget ? `${deleteTarget.nickname || 'Thuốc này'} sẽ bị xóa khỏi hồ sơ và các nhắc nhở liên quan sẽ dừng lại.` : ''}
        open={Boolean(deleteTarget)}
        title="Xóa thuốc khỏi hồ sơ?"
        onConfirm={confirmDeleteMedication}
        onOpenChange={(open) => {
          if (!open) {
            setDeleteTarget(null)
          }
        }}
      />
    </div>
  )
}

function MedicationFormDrawer({
  activeRole,
  form,
  mode,
  patients,
  saving,
  selectedMedication,
  setForm,
  onClose,
  onSubmit,
}) {
  const fileInputRef = useRef(null)
  const [pillResults, setPillResults] = useState([])
  const [pillSearching, setPillSearching] = useState(false)
  const [scanLoading, setScanLoading] = useState(false)
  const [cameraOpen, setCameraOpen] = useState(false)

  useEffect(() => {
    if (mode) {
      setPillResults([])
    }
  }, [mode])

  if (!mode) {
    return null
  }

  const updateField = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const selectPill = (pill) => {
    setForm((current) => ({
      ...current,
      pillId: normalizePillId(pill.id),
      selectedPill: pill,
      pillQuery: pillName(pill, ''),
    }))
  }

  const runPillSearch = async (keyword = form.pillQuery) => {
    const text = keyword.trim()
    if (!text) {
      notify.warning('Nhập tên thuốc để tìm trong danh mục')
      return
    }

    setPillSearching(true)
    try {
      const results = await searchPills(text, { limit: 8 })
      setPillResults(results)
      if (!results.length) {
        notify.info('Không tìm thấy thuốc phù hợp trong danh mục')
      }
    } catch (error) {
      notify.apiError(error, 'Không thể tìm thuốc')
    } finally {
      setPillSearching(false)
    }
  }

  const handleScanFile = async (file) => {
    if (!file) {
      return
    }

    setScanLoading(true)
    try {
      const result = await analyzeMedicationImage(file)
      const candidate = result?.match?.best_match ?? result?.ui?.top_candidates?.[0]
      const candidateName = candidate?.display_name ?? candidate?.title ?? ''
      const candidatePillId = normalizePillId(candidate?.product_id)

      if (!candidateName && !candidatePillId) {
        notify.info('Chưa nhận diện được tên thuốc rõ ràng')
        return
      }

      setForm((current) => ({ ...current, pillQuery: candidateName || current.pillQuery }))
      const results = await searchPills(candidateName || candidatePillId, { limit: 8 })
      setPillResults(results)
      const autoMatch =
        results.find((pill) => normalizePillId(pill.id) === candidatePillId) ||
        results.find((pill) => pillName(pill, '').toLowerCase() === candidateName.toLowerCase())
      if (autoMatch) {
        selectPill(autoMatch)
        notify.success('Đã nhận diện và chọn thuốc')
      } else {
        notify.info('Đã quét xong, chọn thuốc phù hợp trong danh sách')
      }
    } catch (error) {
      notify.error('Quét thuốc thất bại', { description: getApiErrorMessage(error) })
    } finally {
      setScanLoading(false)
    }
  }

  const updateSchedule = (scheduleId, patch) => {
    setForm((current) => ({
      ...current,
      schedules: current.schedules.map((schedule) =>
        schedule.localId === scheduleId ? { ...schedule, ...patch } : schedule,
      ),
    }))
  }

  const updateDose = (scheduleId, doseId, patch) => {
    setForm((current) => ({
      ...current,
      schedules: current.schedules.map((schedule) =>
        schedule.localId === scheduleId
          ? {
              ...schedule,
              times: schedule.times.map((dose) => (dose.localId === doseId ? { ...dose, ...patch } : dose)),
            }
          : schedule,
      ),
    }))
  }

  const toggleWeekday = (scheduleId, weekday) => {
    setForm((current) => ({
      ...current,
      schedules: current.schedules.map((schedule) => {
        if (schedule.localId !== scheduleId) {
          return schedule
        }
        const hasDay = schedule.daysOfWeek.includes(weekday)
        return {
          ...schedule,
          daysOfWeek: hasDay
            ? schedule.daysOfWeek.filter((item) => item !== weekday)
            : [...schedule.daysOfWeek, weekday],
        }
      }),
    }))
  }

  const addSchedule = () => {
    setForm((current) => ({
      ...current,
      schedules: [...current.schedules, newSchedule(current.startDate, current.dosageAmount)],
    }))
  }

  const removeSchedule = (scheduleId) => {
    setForm((current) => ({
      ...current,
      schedules: current.schedules.filter((schedule) => schedule.localId !== scheduleId),
    }))
  }

  const addDose = (scheduleId) => {
    setForm((current) => ({
      ...current,
      schedules: current.schedules.map((schedule) =>
        schedule.localId === scheduleId
          ? { ...schedule, times: [...schedule.times, newDoseTime('20:00', current.dosageAmount)] }
          : schedule,
      ),
    }))
  }

  const removeDose = (scheduleId, doseId) => {
    setForm((current) => ({
      ...current,
      schedules: current.schedules.map((schedule) =>
        schedule.localId === scheduleId
          ? { ...schedule, times: schedule.times.filter((dose) => dose.localId !== doseId) }
          : schedule,
      ),
    }))
  }

  return (
    <>
      <div className="caregiver-medication-overlay fixed inset-0 z-[80] bg-slate-950/35 backdrop-blur-sm" onClick={onClose} />
      <form
        className="caregiver-medication-drawer fixed right-0 top-0 z-[90] grid h-dvh w-[min(920px,100vw)] grid-rows-[auto_minmax(0,1fr)_auto] overflow-hidden border-l border-slate-200 bg-slate-50 shadow-2xl shadow-slate-950/20"
        onSubmit={onSubmit}
      >
        <header className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-200 bg-white p-4 sm:p-5">
          <div>
            <p className="text-xs font-black uppercase text-emerald-700">
              {mode === 'edit' ? 'Cập nhật medication' : 'Tạo medication'}
            </p>
            <h2 className="mt-1 text-2xl font-black leading-tight text-slate-950">
              {mode === 'edit' ? selectedMedication?.nickname || 'Cập nhật thuốc' : 'Thêm thuốc cho hồ sơ'}
            </h2>
          </div>
          <button
            aria-label="Đóng"
            className="grid h-10 w-10 place-items-center rounded-lg border border-slate-200 bg-white text-slate-600 transition hover:bg-slate-50"
            type="button"
            onClick={onClose}
          >
            <X size={20} />
          </button>
        </header>

        <div className="overflow-y-auto p-4 sm:p-5">
          <div className="grid gap-5">
            <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm shadow-slate-100">
              <div className="grid gap-4 lg:grid-cols-[minmax(0,0.42fr)_minmax(0,1fr)]">
                <label className={labelClass}>
                  Hồ sơ bệnh nhân
                  <select
                    className={fieldClass}
                    disabled={activeRole !== 'CAREGIVER'}
                    value={form.patientId}
                    onChange={(event) => updateField('patientId', event.target.value)}
                  >
                    {patients.map((profile) => (
                      <option key={patientId(profile)} value={patientId(profile)}>
                        {fullName(profile)}
                      </option>
                    ))}
                  </select>
                </label>

                <div className="grid gap-3">
                  <div className="flex flex-wrap gap-2">
                    <div className="caregiver-pill-search flex min-w-[260px] flex-1 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3">
                      <Search className="text-slate-400" size={18} />
                      <input
                        className="min-h-11 flex-1 border-0 bg-transparent text-sm font-bold text-slate-950 outline-none"
                        placeholder="Tìm tên thuốc trong danh mục"
                        value={form.pillQuery}
                        onChange={(event) => updateField('pillQuery', event.target.value)}
                        onKeyDown={(event) => {
                          if (event.key === 'Enter') {
                            event.preventDefault()
                            runPillSearch()
                          }
                        }}
                      />
                    </div>
                    <Button disabled={pillSearching || !form.pillQuery.trim()} variant="secondary" onClick={() => runPillSearch()}>
                      {pillSearching ? <Loader2 className="animate-spin" size={16} /> : <Search size={16} />}
                      Tìm
                    </Button>
                    <Button disabled={scanLoading} variant="ghost" onClick={() => setCameraOpen(true)}>
                      <Camera size={16} />
                      Camera
                    </Button>
                    <Button disabled={scanLoading} variant="ghost" onClick={() => fileInputRef.current?.click()}>
                      {scanLoading ? <Loader2 className="animate-spin" size={16} /> : <UploadCloud size={16} />}
                      Ảnh
                    </Button>
                    <input
                      ref={fileInputRef}
                      accept="image/*"
                      className="hidden"
                      type="file"
                      onChange={(event) => {
                        const file = event.target.files?.[0]
                        event.target.value = ''
                        handleScanFile(file)
                      }}
                    />
                  </div>

                  {form.selectedPill ? (
                    <div className="flex items-center gap-3 rounded-lg border border-emerald-200 bg-emerald-50 p-3">
                      <PillThumb pill={form.selectedPill} />
                      <div className="min-w-0 flex-1">
                        <strong className="block truncate text-sm font-black text-emerald-950">
                          {pillName(form.selectedPill)}
                        </strong>
                        <span className="text-xs font-bold text-emerald-700">
                          {form.selectedPill.activeIngredient || form.selectedPill.manufacturer || 'Đã chọn từ Pill Catalog'}
                        </span>
                      </div>
                      <CheckCircle2 className="text-emerald-700" size={20} />
                    </div>
                  ) : null}

                  {pillResults.length ? (
                    <div className="grid gap-2 sm:grid-cols-2">
                      {pillResults.map((pill) => (
                        <button
                          className={cx(
                            'caregiver-pill-result flex min-w-0 items-center gap-3 rounded-lg border p-3 text-left transition hover:border-emerald-300 hover:bg-emerald-50',
                            normalizePillId(form.pillId) === normalizePillId(pill.id)
                              ? 'border-emerald-300 bg-emerald-50'
                              : 'border-slate-200 bg-white',
                          )}
                          key={pill.id}
                          type="button"
                          onClick={() => selectPill(pill)}
                        >
                          <PillThumb pill={pill} />
                          <span className="min-w-0">
                            <strong className="block truncate text-sm font-black text-slate-950">{pillName(pill)}</strong>
                            <small className="block truncate text-xs font-bold text-slate-500">
                              {pill.activeIngredient || pill.dosage || pill.manufacturer || 'Pill Catalog'}
                            </small>
                          </span>
                        </button>
                      ))}
                    </div>
                  ) : null}
                </div>
              </div>
            </section>

            <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm shadow-slate-100">
              <h3 className="text-sm font-black uppercase text-slate-500">Thông tin kê đơn</h3>
              <div className="mt-4 grid gap-4 lg:grid-cols-3">
                <label className={labelClass}>
                  Tên gợi nhớ
                  <input className={fieldClass} value={form.nickname} onChange={(event) => updateField('nickname', event.target.value)} />
                </label>
                <label className={labelClass}>
                  Liều lượng 1 lần
                  <input
                    className={fieldClass}
                    min="0.01"
                    step="0.01"
                    type="number"
                    value={form.dosageAmount}
                    onChange={(event) => updateField('dosageAmount', event.target.value)}
                  />
                </label>
                <label className={labelClass}>
                  Đơn vị
                  <input
                    className={fieldClass}
                    list="medication-unit-options"
                    value={form.dosageUnit}
                    onChange={(event) => updateField('dosageUnit', event.target.value)}
                  />
                </label>
                <label className={labelClass}>
                  Cách dùng
                  <input
                    className={fieldClass}
                    list="medication-route-options"
                    value={form.route}
                    onChange={(event) => updateField('route', event.target.value)}
                  />
                </label>
                <label className={labelClass}>
                  Bữa ăn
                  <select className={fieldClass} value={form.mealRelation} onChange={(event) => updateField('mealRelation', event.target.value)}>
                    {MEAL_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </select>
                </label>
                <label className={labelClass}>
                  Tổng số lượng hiện có
                  <input
                    className={fieldClass}
                    min="1"
                    step="1"
                    type="number"
                    value={form.totalQuantity}
                    onChange={(event) => updateField('totalQuantity', event.target.value)}
                  />
                </label>
                <label className={labelClass}>
                  Ngày bắt đầu
                  <input className={fieldClass} type="date" value={form.startDate} onChange={(event) => updateField('startDate', event.target.value)} />
                </label>
                <label className={labelClass}>
                  Ngày kết thúc
                  <input className={fieldClass} type="date" value={form.endDate} onChange={(event) => updateField('endDate', event.target.value)} />
                </label>
                <label className={labelClass}>
                  Bác sĩ kê đơn
                  <input className={fieldClass} value={form.prescribedBy} onChange={(event) => updateField('prescribedBy', event.target.value)} />
                </label>
                <label className={cx(labelClass, 'lg:col-span-3')}>
                  Mục đích
                  <input className={fieldClass} value={form.purpose} onChange={(event) => updateField('purpose', event.target.value)} />
                </label>
                <label className={cx(labelClass, 'lg:col-span-3')}>
                  Hướng dẫn
                  <textarea
                    className={cx(fieldClass, 'min-h-24 resize-y')}
                    value={form.instruction}
                    onChange={(event) => updateField('instruction', event.target.value)}
                  />
                </label>
              </div>
              <datalist id="medication-unit-options">
                {UNIT_OPTIONS.map((option) => <option key={option} value={option} />)}
              </datalist>
              <datalist id="medication-route-options">
                {ROUTE_OPTIONS.map((option) => <option key={option} value={option} />)}
              </datalist>
            </section>

            <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm shadow-slate-100">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <h3 className="text-sm font-black uppercase text-slate-500">Lịch uống</h3>
                <Button size="sm" variant="secondary" onClick={addSchedule}>
                  <Plus size={15} />
                  Thêm lịch
                </Button>
              </div>

              <div className="mt-4 grid gap-4">
                {form.schedules.map((schedule, index) => (
                  <div className="caregiver-schedule-card rounded-lg border border-slate-200 bg-slate-50 p-3 sm:p-4" key={schedule.localId}>
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <strong className="text-sm font-black text-slate-950">Lịch {index + 1}</strong>
                      <button
                        aria-label="Xóa lịch"
                        className="grid h-9 w-9 place-items-center rounded-lg border border-rose-100 bg-white text-rose-700 transition hover:bg-rose-50"
                        disabled={form.schedules.length === 1}
                        type="button"
                        onClick={() => removeSchedule(schedule.localId)}
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>

                    <div className="mt-3 grid gap-3 lg:grid-cols-4">
                      <label className={labelClass}>
                        Tần suất
                        <select
                          className={fieldClass}
                          value={schedule.frequencyType}
                          onChange={(event) => updateSchedule(schedule.localId, { frequencyType: event.target.value })}
                        >
                          {SCHEDULE_OPTIONS.map((option) => (
                            <option key={option.value} value={option.value}>{option.label}</option>
                          ))}
                        </select>
                      </label>
                      <label className={labelClass}>
                        Khoảng lặp
                        <input
                          className={fieldClass}
                          min="1"
                          step="1"
                          type="number"
                          value={schedule.interval}
                          onChange={(event) => updateSchedule(schedule.localId, { interval: event.target.value })}
                        />
                      </label>
                      <label className={labelClass}>
                        Ngày bắt đầu lịch
                        <input
                          className={fieldClass}
                          type="date"
                          value={schedule.startDate}
                          onChange={(event) => updateSchedule(schedule.localId, { startDate: event.target.value })}
                        />
                      </label>
                      <label className={labelClass}>
                        Ngày kết thúc lịch
                        <input
                          className={fieldClass}
                          type="date"
                          value={schedule.endDate}
                          onChange={(event) => updateSchedule(schedule.localId, { endDate: event.target.value })}
                        />
                      </label>
                    </div>

                    {schedule.frequencyType === 'WEEKLY' ? (
                      <div className="mt-3 flex flex-wrap gap-2">
                        {WEEKDAY_OPTIONS.map((day) => (
                          <button
                            className={cx(
                              'caregiver-weekday-button h-9 rounded-lg border px-3 text-xs font-black transition',
                              schedule.daysOfWeek.includes(day.value)
                                ? 'border-emerald-300 bg-emerald-100 text-emerald-900'
                                : 'border-slate-200 bg-white text-slate-500 hover:border-emerald-200',
                            )}
                            key={day.value}
                            type="button"
                            onClick={() => toggleWeekday(schedule.localId, day.value)}
                          >
                            {day.label}
                          </button>
                        ))}
                      </div>
                    ) : null}

                    <div className="mt-3 grid gap-3 lg:grid-cols-[minmax(0,1fr)_minmax(170px,0.35fr)]">
                      <label className={labelClass}>
                        Ghi chú lịch
                        <input
                          className={fieldClass}
                          value={schedule.note}
                          onChange={(event) => updateSchedule(schedule.localId, { note: event.target.value })}
                        />
                      </label>
                      <label className={labelClass}>
                        Nhắc trước phút
                        <input
                          className={fieldClass}
                          min="0"
                          step="1"
                          type="number"
                          value={schedule.reminderMinutesBefore}
                          onChange={(event) => updateSchedule(schedule.localId, { reminderMinutesBefore: event.target.value })}
                        />
                      </label>
                    </div>

                    <div className="mt-4 grid gap-2">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="text-xs font-black uppercase text-slate-500">Khung giờ</span>
                        <Button size="sm" variant="ghost" onClick={() => addDose(schedule.localId)}>
                          <Plus size={15} />
                          Thêm giờ
                        </Button>
                      </div>
                      <div className="grid gap-2 sm:grid-cols-2">
                        {schedule.times.map((dose) => (
                          <div className="grid grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto] gap-2 rounded-lg border border-slate-200 bg-white p-2" key={dose.localId}>
                            <input
                              aria-label="Giờ uống"
                              className={fieldClass}
                              type="time"
                              value={dose.timeOfDay}
                              onChange={(event) => updateDose(schedule.localId, dose.localId, { timeOfDay: event.target.value })}
                            />
                            <input
                              aria-label="Liều lượng"
                              className={fieldClass}
                              min="0.01"
                              step="0.01"
                              type="number"
                              value={dose.doseAmount}
                              onChange={(event) => updateDose(schedule.localId, dose.localId, { doseAmount: event.target.value })}
                            />
                            <button
                              aria-label="Xóa giờ uống"
                              className="grid h-11 w-11 place-items-center rounded-lg text-rose-700 transition hover:bg-rose-50"
                              disabled={schedule.times.length === 1}
                              type="button"
                              onClick={() => removeDose(schedule.localId, dose.localId)}
                            >
                              <Trash2 size={16} />
                            </button>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </div>
        </div>

        <footer className="flex flex-wrap justify-end gap-2 border-t border-slate-200 bg-white p-4 sm:p-5">
          <Button disabled={saving} variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button disabled={saving} type="submit" variant="primary">
            {saving ? <Loader2 className="animate-spin" size={17} /> : <CheckCircle2 size={17} />}
            {mode === 'edit' ? 'Lưu thay đổi' : 'Lưu thuốc'}
          </Button>
        </footer>
      </form>

      <CameraCapture
        open={cameraOpen}
        onCapture={handleScanFile}
        onClose={() => setCameraOpen(false)}
      />
    </>
  )
}

export default MedicationsPage
