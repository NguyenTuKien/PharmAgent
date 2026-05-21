import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  AtSign,
  CalendarDays,
  Camera,
  Check,
  Eye,
  ImagePlus,
  KeyRound,
  LoaderCircle,
  MapPin,
  MonitorSmartphone,
  Pencil,
  Phone,
  Plus,
  Save,
  ShieldCheck,
  Trash2,
  UploadCloud,
  UserRound,
  X,
} from 'lucide-react'

import { CameraCapture } from '../components/ui/CameraCapture.jsx'
import { ConfirmDialog } from '../components/ui/Modal.jsx'
import { compressImage } from '../lib/imageCompressor.js'
import { notify } from '../lib/toast.js'
import { uploadImageToCloudinary } from '../lib/uploadImage.js'
import { useAuthStore } from '../modules/auth/authStore.js'
import {
  addMyDevice,
  changePassword,
  deleteMyDevice,
  getMyDevices,
  getMyProfile,
  updateMyAvatar,
  updateMyDevice,
  updateMyProfile,
} from '../modules/profile/profileApi.js'

const EMPTY_PROFILE_FORM = {
  firstName: '',
  lastName: '',
  phone: '',
  dateOfBirth: '',
  gender: 'OTHER',
  address: '',
  avatarUrl: '',
}

const EMPTY_PASSWORD_FORM = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
}

const EMPTY_DEVICE_FORM = {
  deviceName: '',
  deviceToken: '',
  deviceType: 'WEB',
  active: true,
}

const ROLE_LABELS = {
  ADMIN: 'Quản trị viên',
  CAREGIVER: 'Người chăm sóc',
  ELDERLY: 'Người thân',
}

const GENDER_LABELS = {
  MALE: 'Nam',
  FEMALE: 'Nữ',
  OTHER: 'Khác',
}

const DEVICE_TYPE_LABELS = {
  ANDROID: 'Android',
  IOS: 'iOS',
  DESKTOP: 'Máy tính',
  WEB: 'Trình duyệt web',
}

const inputClass =
  'min-h-11 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-950 outline-none transition focus:border-emerald-400 focus:ring-4 focus:ring-emerald-100 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-500'
const textareaClass = `${inputClass} min-h-28 resize-y`
const labelClass = 'grid gap-2 text-sm font-bold text-slate-600'
const cardClass = 'rounded-lg border border-slate-200 bg-white shadow-lg shadow-slate-200/50'

function cx(...classes) {
  return classes.filter(Boolean).join(' ')
}

function profileName(profile) {
  return [profile?.firstName, profile?.lastName].filter(Boolean).join(' ').trim() || 'Hồ sơ PharmAgent'
}

function toProfileForm(profile) {
  return {
    firstName: profile?.firstName ?? '',
    lastName: profile?.lastName ?? '',
    phone: profile?.phone ?? '',
    dateOfBirth: profile?.dateOfBirth ?? '',
    gender: profile?.gender ?? 'OTHER',
    address: profile?.address ?? '',
    avatarUrl: profile?.avatarUrl ?? '',
  }
}

function normalizeOptional(value) {
  const trimmed = typeof value === 'string' ? value.trim() : ''
  return trimmed ? trimmed : null
}

function buildProfilePayload(form) {
  return {
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    phone: form.phone.trim(),
    dateOfBirth: form.dateOfBirth || null,
    gender: form.gender || null,
    address: normalizeOptional(form.address),
    avatarUrl: normalizeOptional(form.avatarUrl),
  }
}

function formatDate(value) {
  if (!value) {
    return 'Chưa cập nhật'
  }
  return new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`))
}

function formatDateTime(value) {
  if (!value) {
    return 'Chưa có dữ liệu'
  }
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function ActionButton({
  children,
  className,
  disabled = false,
  size = 'md',
  tone = 'primary',
  type = 'button',
  ...props
}) {
  const tones = {
    primary: 'border-emerald-600 bg-emerald-600 text-white hover:bg-emerald-700',
    secondary: 'border-sky-200 bg-sky-50 text-sky-700 hover:bg-sky-100',
    ghost: 'border-slate-200 bg-white text-slate-700 hover:border-emerald-200 hover:bg-emerald-50 hover:text-emerald-800',
    danger: 'border-rose-600 bg-rose-600 text-white hover:bg-rose-700',
  }
  const sizes = {
    sm: 'min-h-9 px-3 text-xs',
    md: 'min-h-10 px-4 text-sm',
    lg: 'min-h-12 px-5 text-sm',
  }

  return (
    <button
      className={cx(
        'inline-flex items-center justify-center gap-2 rounded-lg border font-black tracking-normal transition',
        'focus:outline-none focus:ring-4 focus:ring-emerald-100 active:translate-y-px disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-60',
        sizes[size],
        tones[tone],
        className,
      )}
      disabled={disabled}
      type={type}
      {...props}
    >
      {children}
    </button>
  )
}

function IconButton({ children, className, label, ...props }) {
  return (
    <button
      aria-label={label}
      className={cx(
        'grid h-9 w-9 shrink-0 place-items-center rounded-lg border border-slate-200 bg-white text-slate-500 transition',
        'hover:border-emerald-200 hover:bg-emerald-50 hover:text-emerald-700 focus:outline-none focus:ring-4 focus:ring-emerald-100',
        className,
      )}
      type="button"
      {...props}
    >
      {children}
    </button>
  )
}

function ProfileAvatar({ profile, size = 'xl' }) {
  const sizes = {
    md: 'h-14 w-14',
    lg: 'h-20 w-20',
    xl: 'h-28 w-28',
  }
  const iconSizes = {
    md: 24,
    lg: 34,
    xl: 46,
  }

  return (
    <div
      className={cx(
        'grid shrink-0 place-items-center overflow-hidden rounded-lg bg-gradient-to-br from-emerald-100 via-sky-50 to-amber-50 text-emerald-800 ring-1 ring-emerald-100',
        sizes[size],
      )}
    >
      {profile?.avatarUrl ? (
        <img className="h-full w-full object-cover" src={profile.avatarUrl} alt={`Ảnh đại diện ${profileName(profile)}`} />
      ) : (
        <UserRound aria-hidden="true" size={iconSizes[size]} strokeWidth={1.9} />
      )}
    </div>
  )
}

function Field({ children, label }) {
  return (
    <label className={labelClass}>
      <span>{label}</span>
      {children}
    </label>
  )
}

function InfoItem({ icon: Icon, label, value }) {
  return (
    <div className="grid gap-2 rounded-lg bg-slate-50 p-3">
      <span className="inline-flex items-center gap-2 text-xs font-black uppercase text-slate-500">
        <Icon size={14} />
        {label}
      </span>
      <strong className="break-words text-sm font-black text-slate-950">{value || 'Chưa cập nhật'}</strong>
    </div>
  )
}

function AvatarEditor({ disabled, onClear, onFile, profile, uploading, value }) {
  const fileInputRef = useRef(null)
  const [cameraOpen, setCameraOpen] = useState(false)
  const previewProfile = { ...profile, avatarUrl: value }

  function handleFileChange(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (file) {
      onFile(file)
    }
  }

  return (
    <div className="grid gap-4 rounded-lg border border-emerald-100 bg-gradient-to-br from-emerald-50 via-white to-sky-50 p-4 sm:grid-cols-[auto_minmax(0,1fr)] sm:items-center">
      <ProfileAvatar profile={previewProfile} />
      <div className="grid gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <span className="inline-flex min-h-8 items-center rounded-full border border-emerald-200 bg-white px-3 text-xs font-black text-emerald-800">
            Ảnh đại diện
          </span>
          {uploading ? (
            <span className="inline-flex items-center gap-2 text-xs font-bold text-slate-500">
              <LoaderCircle className="animate-spin" size={14} />
              Đang cập nhật
            </span>
          ) : null}
        </div>
        <div className="flex flex-wrap gap-2">
          <ActionButton disabled={disabled || uploading} size="sm" tone="secondary" onClick={() => fileInputRef.current?.click()}>
            {uploading ? <LoaderCircle className="animate-spin" size={14} /> : <ImagePlus size={14} />}
            Tải ảnh
          </ActionButton>
          <ActionButton disabled={disabled || uploading} size="sm" tone="ghost" onClick={() => setCameraOpen(true)}>
            <Camera size={14} />
            Chụp ảnh
          </ActionButton>
          {value ? (
            <ActionButton disabled={disabled || uploading} size="sm" tone="ghost" onClick={onClear}>
              <X size={14} />
              Bỏ ảnh
            </ActionButton>
          ) : null}
        </div>
        {value ? (
          <span className="inline-flex items-center gap-2 text-xs font-bold text-emerald-800">
            <UploadCloud size={14} />
            Avatar đang được dùng cho hồ sơ này
          </span>
        ) : (
          <span className="inline-flex items-center gap-2 text-xs font-bold text-slate-500">
            <UserRound size={14} />
            Đang dùng ảnh mặc định
          </span>
        )}
      </div>
      <input ref={fileInputRef} accept="image/*" className="sr-only" type="file" onChange={handleFileChange} />
      <CameraCapture open={cameraOpen} onClose={() => setCameraOpen(false)} onCapture={onFile} />
    </div>
  )
}

function buildDevicePayload(form) {
  return {
    deviceName: form.deviceName.trim(),
    deviceToken: form.deviceToken.trim(),
    deviceType: form.deviceType,
    active: Boolean(form.active),
  }
}

function maskDeviceToken(token) {
  if (!token) {
    return 'Chưa cập nhật'
  }

  const visible = token.slice(-8)
  return `${'•'.repeat(Math.min(8, Math.max(token.length - visible.length, 0)))}${visible}`
}

function DeviceStatusBadge({ active }) {
  return (
    <span
      className={cx(
        'inline-flex min-h-7 items-center rounded-full border px-2.5 text-xs font-black',
        active
          ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
          : 'border-slate-200 bg-slate-50 text-slate-500',
      )}
    >
      {active ? 'Đang hoạt động' : 'Đã tắt'}
    </span>
  )
}

function DeviceCard({ device, onDelete, onEdit }) {
  return (
    <article className="flex items-center justify-between gap-3 rounded-lg border border-slate-200 bg-white p-3">
      <div className="flex min-w-0 items-center gap-3">
        <div className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-emerald-50 text-emerald-700">
          <MonitorSmartphone size={18} />
        </div>
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <strong className="block truncate text-sm font-black text-slate-950">{device.deviceName}</strong>
            <DeviceStatusBadge active={device.active} />
          </div>
          <span className="mt-1 block truncate text-sm font-bold text-slate-500">
            {DEVICE_TYPE_LABELS[device.deviceType] ?? device.deviceType} · {maskDeviceToken(device.deviceToken)}
          </span>
          <span className="mt-1 block text-xs font-bold text-slate-400">
            Lần cuối hoạt động: {formatDateTime(device.lastSeenAt)}
          </span>
        </div>
      </div>
      <div className="flex shrink-0 gap-2">
        <IconButton label="Sửa thiết bị" onClick={() => onEdit(device)}>
          <Pencil size={16} />
        </IconButton>
        <IconButton className="hover:border-rose-200 hover:bg-rose-50 hover:text-rose-700" label="Xóa thiết bị" onClick={() => onDelete(device)}>
          <Trash2 size={16} />
        </IconButton>
      </div>
    </article>
  )
}

export function ProfileSettingsPage() {
  const activeProfile = useAuthStore((state) => state.activeProfile)
  const mergeActiveProfile = useAuthStore((state) => state.mergeActiveProfile)
  const [profile, setProfile] = useState(null)
  const [profileForm, setProfileForm] = useState(EMPTY_PROFILE_FORM)
  const [passwordForm, setPasswordForm] = useState(EMPTY_PASSWORD_FORM)
  const [devices, setDevices] = useState([])
  const [deviceForm, setDeviceForm] = useState(EMPTY_DEVICE_FORM)
  const [editingDeviceId, setEditingDeviceId] = useState('')
  const [deleteDeviceTarget, setDeleteDeviceTarget] = useState(null)
  const [loading, setLoading] = useState(true)
  const [savingProfile, setSavingProfile] = useState(false)
  const [savingPassword, setSavingPassword] = useState(false)
  const [savingDevice, setSavingDevice] = useState(false)
  const [uploadingAvatar, setUploadingAvatar] = useState(false)

  const role = profile?.role ?? activeProfile?.role
  const isReadOnly = role === 'ELDERLY'

  const roleLabel = useMemo(() => ROLE_LABELS[role?.toUpperCase?.()] ?? 'Hồ sơ', [role])

  const loadSettings = useCallback(async () => {
    setLoading(true)
    try {
      const [profileData, deviceData] = await Promise.all([getMyProfile(), getMyDevices()])
      setProfile(profileData)
      setProfileForm(toProfileForm(profileData))
      setDevices(Array.isArray(deviceData) ? deviceData : [])
    } catch (err) {
      notify.apiError(err, 'Không thể tải hồ sơ cá nhân')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadSettings()
  }, [loadSettings])

  const setProfileField = (field) => (event) => {
    setProfileForm((current) => ({ ...current, [field]: event.target.value }))
  }

  const setPasswordField = (field) => (event) => {
    setPasswordForm((current) => ({ ...current, [field]: event.target.value }))
  }

  const setDeviceField = (field) => (event) => {
    setDeviceForm((current) => ({ ...current, [field]: event.target.value }))
  }

  const setDeviceActive = (event) => {
    setDeviceForm((current) => ({ ...current, active: event.target.checked }))
  }

  function syncProfile(updatedProfile) {
    setProfile(updatedProfile)
    setProfileForm(toProfileForm(updatedProfile))
    mergeActiveProfile(updatedProfile)
  }

  async function handleAvatarFile(file) {
    if (isReadOnly) {
      return
    }

    setUploadingAvatar(true)
    try {
      const compressed = await compressImage(file, { maxWidth: 720, maxHeight: 720, quality: 0.82 })
      const avatarUrl = await uploadImageToCloudinary(compressed, 'avatar')
      const updated = await updateMyAvatar(avatarUrl)
      syncProfile(updated)
      notify.success('Đã cập nhật ảnh đại diện')
    } catch (err) {
      notify.apiError(err, 'Không thể cập nhật ảnh đại diện')
    } finally {
      setUploadingAvatar(false)
    }
  }

  async function clearAvatar() {
    if (isReadOnly || uploadingAvatar) {
      return
    }

    setUploadingAvatar(true)
    try {
      const updated = await updateMyAvatar(null)
      syncProfile(updated)
      notify.success('Đã bỏ ảnh đại diện')
    } catch (err) {
      notify.apiError(err, 'Không thể bỏ ảnh đại diện')
    } finally {
      setUploadingAvatar(false)
    }
  }

  async function handleSaveProfile(event) {
    event.preventDefault()

    if (isReadOnly) {
      return
    }

    if (uploadingAvatar) {
      notify.warning('Vui lòng chờ ảnh đại diện cập nhật xong')
      return
    }

    if (!profileForm.firstName.trim() || !profileForm.lastName.trim() || !profileForm.phone.trim()) {
      notify.warning('Vui lòng nhập đầy đủ họ, tên và số điện thoại')
      return
    }

    setSavingProfile(true)
    try {
      const updated = await updateMyProfile(buildProfilePayload(profileForm))
      syncProfile(updated)
      notify.success('Đã cập nhật hồ sơ cá nhân')
    } catch (err) {
      notify.apiError(err, 'Không thể cập nhật hồ sơ')
    } finally {
      setSavingProfile(false)
    }
  }

  async function handleChangePassword(event) {
    event.preventDefault()

    if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
      notify.warning('Vui lòng nhập đầy đủ thông tin mật khẩu')
      return
    }

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      notify.warning('Mật khẩu mới và xác nhận mật khẩu không khớp')
      return
    }

    setSavingPassword(true)
    try {
      await changePassword(passwordForm)
      setPasswordForm(EMPTY_PASSWORD_FORM)
      notify.success('Đã đổi mật khẩu tài khoản')
    } catch (err) {
      notify.apiError(err, 'Không thể đổi mật khẩu')
    } finally {
      setSavingPassword(false)
    }
  }

  function startEditDevice(device) {
    setEditingDeviceId(device.id)
    setDeviceForm({
      deviceName: device.deviceName ?? '',
      deviceToken: device.deviceToken ?? '',
      deviceType: device.deviceType ?? 'WEB',
      active: device.active ?? true,
    })
  }

  function resetDeviceForm() {
    setEditingDeviceId('')
    setDeviceForm(EMPTY_DEVICE_FORM)
  }

  async function handleSaveDevice(event) {
    event.preventDefault()

    if (!deviceForm.deviceName.trim() || !deviceForm.deviceToken.trim()) {
      notify.warning('Vui lòng nhập tên thiết bị và mã thiết bị')
      return
    }

    setSavingDevice(true)
    try {
      const payload = buildDevicePayload(deviceForm)
      const savedDevice = editingDeviceId
        ? await updateMyDevice(editingDeviceId, payload)
        : await addMyDevice(payload)

      setDevices((current) => {
        const withoutSavedDevice = current.filter((device) => device.id !== savedDevice.id)
        return editingDeviceId
          ? current.map((device) => (device.id === savedDevice.id ? savedDevice : device))
          : [savedDevice, ...withoutSavedDevice]
      })
      resetDeviceForm()
      notify.success(editingDeviceId ? 'Đã cập nhật thiết bị' : 'Đã thêm thiết bị')
    } catch (err) {
      notify.apiError(err, 'Không thể lưu thiết bị')
    } finally {
      setSavingDevice(false)
    }
  }

  async function handleDeleteDevice() {
    if (!deleteDeviceTarget) {
      return
    }

    try {
      await deleteMyDevice(deleteDeviceTarget.id)
      setDevices((current) => current.filter((device) => device.id !== deleteDeviceTarget.id))
      if (editingDeviceId === deleteDeviceTarget.id) {
        resetDeviceForm()
      }
      notify.success('Đã xóa thiết bị')
    } catch (err) {
      notify.apiError(err, 'Không thể xóa thiết bị')
    } finally {
      setDeleteDeviceTarget(null)
    }
  }

  if (loading) {
    return (
      <div className="mx-auto grid min-h-[420px] w-full max-w-[1480px] place-items-center rounded-lg border border-slate-200 bg-white p-6 shadow-lg shadow-slate-200/50">
        <div className="inline-flex items-center gap-3 text-sm font-black text-slate-600">
          <LoaderCircle className="animate-spin text-emerald-600" size={24} />
          Đang tải hồ sơ
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto grid w-full max-w-[1480px] gap-5 p-4 sm:p-6 lg:p-8">
      <section className="overflow-hidden rounded-lg border border-emerald-100 bg-gradient-to-br from-emerald-50 via-white to-sky-50 p-5 shadow-lg shadow-slate-200/60 lg:p-7">
        <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-center">
          <div className="flex min-w-0 flex-col gap-4 sm:flex-row sm:items-center">
            <ProfileAvatar profile={profile} />
            <div className="min-w-0">
              <p className="text-xs font-black uppercase text-emerald-700">Cài đặt cá nhân</p>
              <h1 className="mt-2 text-3xl font-black leading-tight text-slate-950 sm:text-4xl">{profileName(profile)}</h1>
              <div className="mt-3 flex flex-wrap gap-2">
                <span className="inline-flex min-h-8 items-center gap-2 rounded-full border border-emerald-200 bg-white/80 px-3 text-xs font-black text-emerald-800">
                  <ShieldCheck size={14} />
                  {roleLabel}
                </span>
                {isReadOnly ? (
                  <span className="inline-flex min-h-8 items-center gap-2 rounded-full border border-sky-200 bg-sky-50 px-3 text-xs font-black text-sky-700">
                    <Eye size={14} />
                    Chỉ xem
                  </span>
                ) : null}
              </div>
            </div>
          </div>
          <div className="grid min-w-[220px] gap-2 rounded-lg border border-white/80 bg-white/80 p-4 text-sm shadow-sm shadow-slate-200/70">
            <span className="text-xs font-black uppercase text-slate-500">Cập nhật gần nhất</span>
            <strong className="font-black text-slate-950">{formatDateTime(profile?.updatedAt ?? profile?.createdAt)}</strong>
          </div>
        </div>
      </section>

      <section className="grid gap-5 xl:grid-cols-[minmax(0,1.15fr)_minmax(340px,0.85fr)]">
        <article className={cx(cardClass, 'grid content-start gap-5 p-5')}>
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-xs font-black uppercase text-emerald-700">Thông tin hồ sơ</p>
              <h2 className="mt-1 text-xl font-black text-slate-950">
                {isReadOnly ? 'Thông tin cá nhân' : 'Chỉnh sửa thông tin cá nhân'}
              </h2>
            </div>
            {isReadOnly ? <Eye className="text-sky-600" size={22} /> : <Pencil className="text-emerald-600" size={22} />}
          </div>

          {isReadOnly ? (
            <div className="grid gap-3 sm:grid-cols-2">
              <InfoItem icon={UserRound} label="Họ và tên" value={profileName(profile)} />
              <InfoItem icon={Phone} label="Số điện thoại" value={profile?.phone} />
              <InfoItem icon={CalendarDays} label="Ngày sinh" value={formatDate(profile?.dateOfBirth)} />
              <InfoItem icon={ShieldCheck} label="Giới tính" value={GENDER_LABELS[profile?.gender]} />
              <InfoItem icon={MapPin} label="Địa chỉ" value={profile?.address} />
            </div>
          ) : (
            <form className="grid gap-5" onSubmit={handleSaveProfile}>
              <AvatarEditor
                disabled={savingProfile}
                profile={profile}
                uploading={uploadingAvatar}
                value={profileForm.avatarUrl}
                onClear={clearAvatar}
                onFile={handleAvatarFile}
              />

              <div className="grid gap-4 sm:grid-cols-2">
                <Field label="Họ">
                  <input className={inputClass} value={profileForm.firstName} onChange={setProfileField('firstName')} />
                </Field>
                <Field label="Tên">
                  <input className={inputClass} value={profileForm.lastName} onChange={setProfileField('lastName')} />
                </Field>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <Field label="Số điện thoại">
                  <input className={inputClass} value={profileForm.phone} onChange={setProfileField('phone')} />
                </Field>
                <Field label="Ngày sinh">
                  <input className={inputClass} type="date" value={profileForm.dateOfBirth} onChange={setProfileField('dateOfBirth')} />
                </Field>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <Field label="Giới tính">
                  <select className={inputClass} value={profileForm.gender} onChange={setProfileField('gender')}>
                    <option value="MALE">Nam</option>
                    <option value="FEMALE">Nữ</option>
                    <option value="OTHER">Khác</option>
                  </select>
                </Field>
              </div>

              <Field label="Địa chỉ">
                <textarea className={textareaClass} value={profileForm.address} onChange={setProfileField('address')} />
              </Field>

              <div className="flex flex-col-reverse gap-3 border-t border-slate-100 pt-4 sm:flex-row sm:justify-end">
                <ActionButton disabled={savingProfile || uploadingAvatar} type="submit">
                  {savingProfile ? <LoaderCircle className="animate-spin" size={16} /> : <Save size={16} />}
                  Lưu hồ sơ
                </ActionButton>
              </div>
            </form>
          )}
        </article>

        <aside className="grid content-start gap-5">
          <article className={cx(cardClass, 'grid gap-4 p-5')}>
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-xs font-black uppercase text-sky-700">Thẻ thông tin</p>
                <h2 className="mt-1 text-xl font-black text-slate-950">Tổng quan hồ sơ</h2>
              </div>
              <ProfileAvatar profile={profile} size="md" />
            </div>
            <div className="grid gap-3">
              <InfoItem icon={AtSign} label="Mã hồ sơ" value={profile?.id} />
              <InfoItem icon={ShieldCheck} label="Vai trò" value={roleLabel} />
              <InfoItem icon={CalendarDays} label="Ngày tạo" value={formatDateTime(profile?.createdAt)} />
            </div>
          </article>

          <article className={cx(cardClass, 'grid gap-4 p-5')}>
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-xs font-black uppercase text-emerald-700">Tài khoản</p>
                <h2 className="mt-1 text-xl font-black text-slate-950">Bảo mật</h2>
              </div>
              <KeyRound className="text-emerald-600" size={22} />
            </div>

            {isReadOnly ? (
              <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 p-4 text-sm font-bold leading-6 text-slate-600">
                Hồ sơ người thân chỉ có quyền xem trong phiên đang chọn.
              </div>
            ) : (
              <form className="grid gap-4" onSubmit={handleChangePassword}>
                <Field label="Mật khẩu hiện tại">
                  <input className={inputClass} type="password" value={passwordForm.currentPassword} onChange={setPasswordField('currentPassword')} />
                </Field>
                <Field label="Mật khẩu mới">
                  <input className={inputClass} type="password" value={passwordForm.newPassword} onChange={setPasswordField('newPassword')} />
                </Field>
                <Field label="Xác nhận mật khẩu mới">
                  <input className={inputClass} type="password" value={passwordForm.confirmPassword} onChange={setPasswordField('confirmPassword')} />
                </Field>
                <div className="flex justify-end border-t border-slate-100 pt-4">
                  <ActionButton disabled={savingPassword} type="submit">
                    {savingPassword ? <LoaderCircle className="animate-spin" size={16} /> : <KeyRound size={16} />}
                    Đổi mật khẩu
                  </ActionButton>
                </div>
              </form>
            )}
          </article>
        </aside>
      </section>

      <section className={cx(cardClass, 'grid gap-5 p-5')}>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-xs font-black uppercase text-emerald-700">Thiết bị</p>
            <h2 className="mt-1 text-xl font-black text-slate-950">Quản lý thiết bị</h2>
          </div>
          <MonitorSmartphone className="text-emerald-600" size={22} />
        </div>

        <div className="grid gap-5 lg:grid-cols-[minmax(280px,0.42fr)_minmax(0,1fr)]">
          <form className="grid content-start gap-4 rounded-lg border border-slate-100 bg-slate-50/70 p-4" onSubmit={handleSaveDevice}>
            <Field label="Tên thiết bị">
              <input className={inputClass} value={deviceForm.deviceName} onChange={setDeviceField('deviceName')} />
            </Field>
            <Field label="Mã thiết bị">
              <input className={inputClass} value={deviceForm.deviceToken} onChange={setDeviceField('deviceToken')} />
            </Field>
            <Field label="Loại thiết bị">
              <select className={inputClass} value={deviceForm.deviceType} onChange={setDeviceField('deviceType')}>
                <option value="WEB">Trình duyệt web</option>
                <option value="ANDROID">Android</option>
                <option value="IOS">iOS</option>
                <option value="DESKTOP">Máy tính</option>
              </select>
            </Field>
            <label className="flex min-h-11 items-center justify-between gap-3 rounded-lg border border-slate-200 bg-white px-3 text-sm font-black text-slate-700">
              <span>Thiết bị đang hoạt động</span>
              <input className="h-5 w-5 accent-emerald-600" checked={deviceForm.active} type="checkbox" onChange={setDeviceActive} />
            </label>
            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              {editingDeviceId ? (
                <ActionButton tone="ghost" onClick={resetDeviceForm}>
                  <X size={16} />
                  Hủy
                </ActionButton>
              ) : null}
              <ActionButton disabled={savingDevice} type="submit">
                {savingDevice ? <LoaderCircle className="animate-spin" size={16} /> : editingDeviceId ? <Check size={16} /> : <Plus size={16} />}
                {editingDeviceId ? 'Lưu thiết bị' : 'Thêm thiết bị'}
              </ActionButton>
            </div>
          </form>

          <div className="grid content-start gap-3">
            {devices.length ? (
              devices.map((device) => (
                <DeviceCard
                  device={device}
                  key={device.id}
                  onDelete={setDeleteDeviceTarget}
                  onEdit={startEditDevice}
                />
              ))
            ) : (
              <div className="grid min-h-36 place-items-center rounded-lg border border-dashed border-slate-200 bg-slate-50 p-6 text-center text-sm font-bold text-slate-500">
                Chưa có thiết bị nào được lưu.
              </div>
            )}
          </div>
        </div>
      </section>

      <ConfirmDialog
        confirmLabel="Xóa thiết bị"
        description={`Bạn có chắc muốn xóa thiết bị "${deleteDeviceTarget?.deviceName}"?`}
        open={Boolean(deleteDeviceTarget)}
        title="Xóa thiết bị"
        onConfirm={handleDeleteDevice}
        onOpenChange={(open) => {
          if (!open) {
            setDeleteDeviceTarget(null)
          }
        }}
      />
    </div>
  )
}

export default ProfileSettingsPage
