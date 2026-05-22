import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  CalendarClock,
  Camera,
  Check,
  Eye,
  HeartHandshake,
  ImagePlus,
  LoaderCircle,
  MapPin,
  MessageCircle,
  NotebookText,
  Pencil,
  Phone,
  Pill,
  Plus,
  Search,
  Send,
  ShieldCheck,
  Trash2,
  UploadCloud,
  UserRound,
  X,
} from 'lucide-react'

import { CameraCapture } from '../../components/ui/CameraCapture.jsx'
import { compressImage } from '../../lib/imageCompressor.js'
import { notify } from '../../lib/toast.js'
import { uploadImageToCloudinary } from '../../lib/uploadImage.js'
import { useAuthStore } from '../../modules/auth/authStore.js'
import {
  acceptCaregiverInvitation,
  createManagedElderlyProfile,
  deleteCaregiverRelationship,
  deleteManagedElderlyProfile,
  getCaregiverRelationships,
  getElderlyRelationships,
  getManagedElderlyProfile,
  getMyProfile,
  getPendingCaregiverRelationships,
  getPendingElderlyRelationships,
  getProfiles,
  inviteElderlyProfile,
  refuseCaregiverInvitation,
  searchElderlyProfiles,
  updateCaregiverRelationship,
  updateManagedElderlyProfile,
} from '../../modules/profile/profileApi.js'

const EMPTY_PROFILE_FORM = {
  firstName: '',
  lastName: '',
  phone: '',
  dateOfBirth: '',
  gender: 'OTHER',
  address: '',
  avatarUrl: '',
}

const EMPTY_INVITE_FORM = {
  caregiverTitle: 'Người chăm sóc',
  relation: 'FATHER',
  customRelation: '',
}

const FAMILY_RELATION_OPTIONS = [
  { value: 'FATHER', label: 'Bố' },
  { value: 'MOTHER', label: 'Mẹ' },
  { value: 'PATERNAL_GRANDFATHER', label: 'Ông Nội' },
  { value: 'PATERNAL_GRANDMOTHER', label: 'Bà Nội' },
  { value: 'MATERNAL_GRANDFATHER', label: 'Ông Ngoại' },
  { value: 'MATERNAL_GRANDMOTHER', label: 'Bà Ngoại' },
  { value: 'OTHER', label: 'Khác' },
]

const FAMILY_RELATION_LABELS = FAMILY_RELATION_OPTIONS.reduce((labels, option) => {
  labels[option.value] = option.label
  return labels
}, {})

const PROFILE_ROLE_LABELS = {
  ADMIN: 'Quản trị',
  CAREGIVER: 'Người chăm sóc',
  ELDERLY: 'Người thân',
}

const STATUS_LABELS = {
  ACCEPTED: 'Đã xác nhận',
  PENDING: 'Đang chờ',
  LOCAL: 'Hồ sơ đã tạo',
  REVOKED: 'Đã thu hồi',
  REFUSED: 'Đã từ chối',
}

const RELATIONSHIP_FILTERS = [
  { value: 'ACCEPTED', label: 'Đã xác nhận' },
  { value: 'PENDING', label: 'Đang chờ' },
  { value: 'LOCAL', label: 'Hồ sơ đã tạo' },
]

const GENDER_LABELS = {
  MALE: 'Nam',
  FEMALE: 'Nữ',
  OTHER: 'Khác',
}

const inputClass =
  'min-h-11 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-900 outline-none transition focus:border-emerald-400 focus:ring-4 focus:ring-emerald-100'
const textareaClass = `${inputClass} min-h-24 resize-y`
const labelClass = 'grid gap-2 text-sm font-bold text-slate-600'
const panelClass = 'rounded-lg border border-slate-200 bg-white/95 shadow-lg shadow-slate-200/50'

function cx(...classes) {
  return classes.filter(Boolean).join(' ')
}

function fullName(profile) {
  return [profile?.firstName, profile?.lastName].filter(Boolean).join(' ').trim() || 'Hồ sơ PharmAgent'
}

function profileToForm(profile) {
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

function buildProfilePayload(form, relationForm = { relation: 'OTHER', customRelation: '' }) {
  return {
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    phone: form.phone.trim(),
    dateOfBirth: form.dateOfBirth || null,
    gender: form.gender,
    address: normalizeOptional(form.address),
    avatarUrl: normalizeOptional(form.avatarUrl),
    role: 'ELDERLY',
    ...relationPayload(relationForm),
  }
}

function asPageContent(page) {
  return Array.isArray(page?.content) ? page.content : []
}

function normalizeRelationship(profile, fallbackStatus = 'ACCEPTED') {
  const status = (profile?.status || fallbackStatus).toUpperCase()

  return {
    ...profile,
    profileId: profile?.profileId ?? profile?.id,
    relation: profile?.relation ?? 'OTHER',
    relationLabel: relationshipLabel(profile),
    status,
  }
}

function relationshipLabel(profile) {
  if (!profile) {
    return 'Chưa cập nhật'
  }

  const relation = profile.relation ?? 'OTHER'
  const customRelation = normalizeOptional(profile.customRelation)
  if (relation === 'OTHER' && customRelation) {
    return customRelation
  }
  return profile.relationLabel || FAMILY_RELATION_LABELS[relation] || profile.elderlyTitle || profile.caregiverTitle || 'Chưa cập nhật'
}

function relationLabelFromPayload(relation, customRelation, fallback) {
  const normalizedRelation = relation || 'OTHER'
  const normalizedCustomRelation = normalizeOptional(customRelation)

  if (normalizedRelation === 'OTHER' && normalizedCustomRelation) {
    return normalizedCustomRelation
  }

  return FAMILY_RELATION_LABELS[normalizedRelation] || fallback || FAMILY_RELATION_LABELS.OTHER
}

function relationPayload(form) {
  return {
    relation: form.relation || 'OTHER',
    customRelation: form.relation === 'OTHER' ? normalizeOptional(form.customRelation) : null,
  }
}

function applyRelationPayload(profile, payload) {
  const relation = payload.relation || 'OTHER'
  const customRelation = relation === 'OTHER' ? normalizeOptional(payload.customRelation) : null
  const relationLabel = relationLabelFromPayload(relation, customRelation, profile?.relationLabel)

  return {
    ...profile,
    relation,
    customRelation,
    elderlyTitle: relationLabel,
    relationLabel,
  }
}

function relationFormFromProfile(profile) {
  return {
    relation: profile?.relation ?? 'OTHER',
    customRelation: profile?.customRelation ?? '',
  }
}

function isManagedLocalProfile(profile) {
  return profile?.source === 'local'
}

function canEditProfileFields(profile) {
  return !profile || isManagedLocalProfile(profile)
}

function canEditProfileRelation() {
  return true
}

function profileKey(profile) {
  return profile?.profileId ?? profile?.id
}

function relationshipKey(profile) {
  return profileKey(profile) ?? profile?.relationshipId
}

function profileQuery(profile, action) {
  const params = new URLSearchParams()
  const profileId = relationshipKey(profile)

  if (profileId) {
    params.set('profileId', profileId)
  }

  if (action) {
    params.set('action', action)
  }

  const suffix = params.toString()
  return suffix ? `?${suffix}` : ''
}

function statusLabel(status) {
  return STATUS_LABELS[(status || '').toUpperCase()] ?? 'Chưa rõ'
}

function AppButton({
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
    amber: 'border-amber-200 bg-amber-50 text-amber-800 hover:bg-amber-100',
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
        'focus:outline-none focus:ring-4 focus:ring-emerald-100',
        sizes[size],
        tones[tone],
        disabled && 'pointer-events-none cursor-not-allowed opacity-60',
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

function ProfileBadge({ profile, size = 'md' }) {
  const sizes = {
    sm: 'h-10 w-10 text-xs',
    md: 'h-12 w-12 text-sm',
    lg: 'h-16 w-16 text-base',
    xl: 'h-24 w-24 text-base',
  }
  const iconSizes = {
    sm: 18,
    md: 22,
    lg: 30,
    xl: 42,
  }

  return (
    <div
      className={cx(
        'grid shrink-0 place-items-center overflow-hidden rounded-lg bg-gradient-to-br from-emerald-100 to-sky-100 font-black text-emerald-800 ring-1 ring-emerald-100',
        sizes[size],
      )}
    >
      {profile?.avatarUrl ? (
        <img className="h-full w-full object-cover" src={profile.avatarUrl} alt={`Ảnh đại diện ${fullName(profile)}`} />
      ) : (
        <UserRound aria-hidden="true" size={iconSizes[size] ?? 24} strokeWidth={1.9} />
      )}
    </div>
  )
}

function AvatarPicker({ disabled, onClear, onFile, profile, uploading, value }) {
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
    <div className="grid gap-3 rounded-lg border border-emerald-100 bg-emerald-50/70 p-4 sm:grid-cols-[auto_minmax(0,1fr)] sm:items-center">
      <ProfileBadge profile={previewProfile} size="xl" />
      <div className="grid gap-3">
        <div>
          <p className="text-xs font-black uppercase text-emerald-700">Ảnh đại diện</p>
          <p className="mt-1 text-sm font-semibold leading-6 text-slate-600">
            Tải ảnh từ máy hoặc chụp trực tiếp. Khi chưa có ảnh, hồ sơ sẽ dùng hình người mặc định.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <AppButton disabled={disabled || uploading} size="sm" tone="secondary" onClick={() => fileInputRef.current?.click()}>
            {uploading ? <LoaderCircle className="animate-spin" size={14} /> : <ImagePlus size={14} />}
            Tải ảnh
          </AppButton>
          <AppButton disabled={disabled || uploading} size="sm" tone="ghost" onClick={() => setCameraOpen(true)}>
            <Camera size={14} />
            Chụp ảnh
          </AppButton>
          {value ? (
            <AppButton disabled={disabled || uploading} size="sm" tone="ghost" onClick={onClear}>
              <X size={14} />
              Bỏ ảnh
            </AppButton>
          ) : null}
        </div>
        {value ? (
          <span className="inline-flex items-center gap-2 text-xs font-bold text-emerald-800">
            <UploadCloud size={14} />
            Ảnh đã sẵn sàng để lưu cùng hồ sơ
          </span>
        ) : null}
      </div>
      <input ref={fileInputRef} accept="image/*" className="sr-only" type="file" onChange={handleFileChange} />
      <CameraCapture open={cameraOpen} onClose={() => setCameraOpen(false)} onCapture={onFile} />
    </div>
  )
}

function RelationSelect({ value, onChange, className, disabled = false }) {
  return (
    <select
      className={cx(inputClass, className)}
      disabled={disabled}
      value={value ?? 'FATHER'}
      onChange={(event) => onChange(event.target.value)}
    >
      {FAMILY_RELATION_OPTIONS.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  )
}

function RelationFields({ customRelation, disabled = false, onCustomRelationChange, onRelationChange, relation }) {
  return (
    <div className="grid gap-4">
      <Field label="Quan hệ">
        <RelationSelect disabled={disabled} value={relation} onChange={onRelationChange} />
      </Field>
      {relation === 'OTHER' ? (
        <Field label="Quan hệ khác">
          <input
            className={inputClass}
            disabled={disabled}
            placeholder="Nhập cách gọi"
            value={customRelation}
            onChange={(event) => onCustomRelationChange(event.target.value)}
          />
        </Field>
      ) : null}
    </div>
  )
}

function EmptyState({ children }) {
  return (
    <div className="grid min-h-36 place-items-center rounded-lg border border-dashed border-slate-200 bg-slate-50/80 p-6 text-center text-sm font-bold text-slate-500">
      {children}
    </div>
  )
}

function LoadingState({ children }) {
  return (
    <div className="grid min-h-[360px] place-items-center gap-3 text-slate-600">
      <LoaderCircle className="animate-spin text-emerald-600" size={26} />
      <span className="text-sm font-bold">{children}</span>
    </div>
  )
}

function SkeletonBlock({ className }) {
  return <span aria-hidden="true" className={cx('block animate-pulse rounded bg-slate-200/80', className)} />
}

function StatusPill({ status }) {
  const normalizedStatus = (status || '').toUpperCase()
  const tones = {
    ACCEPTED: 'border-emerald-200 bg-emerald-50 text-emerald-700',
    PENDING: 'border-sky-200 bg-sky-50 text-sky-700',
    REVOKED: 'border-rose-200 bg-rose-50 text-rose-700',
    REFUSED: 'border-rose-200 bg-rose-50 text-rose-700',
  }

  return (
    <span
      className={cx(
        'inline-flex min-h-8 items-center rounded-full border px-3 text-xs font-black',
        tones[normalizedStatus] ?? 'border-slate-200 bg-slate-50 text-slate-600',
      )}
    >
      {statusLabel(normalizedStatus)}
    </span>
  )
}

function IconActionButton({ children, label, onClick }) {
  return (
    <button
      aria-label={label}
      className="inline-grid h-9 w-9 place-items-center rounded-lg border border-slate-200 bg-white text-slate-600 transition hover:border-emerald-200 hover:bg-emerald-50 hover:text-emerald-800 focus:outline-none focus:ring-4 focus:ring-emerald-100 active:scale-[0.98]"
      title={label}
      type="button"
      onClick={onClick}
    >
      {children}
    </button>
  )
}

function RelationChip({ profile }) {
  return (
    <span className="inline-flex min-h-8 max-w-full items-center rounded-full border border-amber-200 bg-amber-50 px-3 text-xs font-black text-amber-800">
      <span className="truncate">{relationshipLabel(profile)}</span>
    </span>
  )
}

function ContactLine({ icon: Icon, children, className }) {
  return (
    <span className={cx('flex min-w-0 items-start gap-2 text-sm font-semibold', className)}>
      <Icon className="mt-0.5 shrink-0 text-slate-400" size={15} />
      <span className="min-w-0 break-words">{children}</span>
    </span>
  )
}

function RelationshipMobileCard({ isLoading = false, isSelected = false, onView, profile }) {
  if (isLoading) {
    return (
      <article className="grid gap-4 rounded-lg border border-slate-200 bg-white p-4 shadow-sm shadow-slate-100">
        <div className="flex items-start gap-3">
          <SkeletonBlock className="h-12 w-12 rounded-lg" />
          <div className="grid min-w-0 flex-1 gap-2">
            <SkeletonBlock className="h-4 w-40 max-w-full" />
            <SkeletonBlock className="h-3 w-32 max-w-full" />
          </div>
        </div>
        <div className="grid gap-2">
          <SkeletonBlock className="h-4 w-52 max-w-full" />
          <SkeletonBlock className="h-4 w-full max-w-full" />
        </div>
        <div className="flex flex-wrap gap-2">
          <SkeletonBlock className="h-8 w-24 rounded-full" />
          <SkeletonBlock className="h-8 w-28 rounded-full" />
        </div>
      </article>
    )
  }

  return (
    <article
      className={cx(
        'grid gap-4 rounded-lg border bg-white p-4 shadow-sm shadow-slate-100 transition',
        isSelected ? 'border-emerald-300 bg-emerald-50/70' : 'border-slate-200',
      )}
    >
      <button className="flex min-w-0 items-start gap-3 text-left" type="button" onClick={() => onView(profile)}>
        <ProfileBadge profile={profile} />
        <span className="min-w-0 flex-1">
          <strong className="block break-words text-base font-black leading-tight text-slate-950">{fullName(profile)}</strong>
          <small className="mt-1 block text-xs font-bold text-slate-500">
            {GENDER_LABELS[profile.gender] || 'Chưa cập nhật'} · {profile.dateOfBirth || 'Chưa có ngày sinh'}
          </small>
        </span>
      </button>

      <div className="grid gap-2 rounded-lg bg-slate-50/80 p-3 text-slate-700">
        <ContactLine icon={Phone}>{profile.phone || 'Chưa cập nhật số điện thoại'}</ContactLine>
        <ContactLine icon={MapPin} className="text-slate-500">
          {profile.address || 'Chưa cập nhật địa chỉ'}
        </ContactLine>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <RelationChip profile={profile} />
        <StatusPill status={profile.status} />
      </div>

      <AppButton className="w-full" tone="ghost" onClick={() => onView(profile)}>
        <Eye size={16} />
        Mở chi tiết
      </AppButton>
    </article>
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

function CenteredCardShell({ children, description, eyebrow, maxWidth = 'max-w-4xl', onClose, open, title }) {
  if (!open) {
    return null
  }

  return (
    <>
      <div className="fixed inset-0 z-[80] bg-slate-950/40 backdrop-blur-sm" onClick={onClose} />
      <section
        aria-modal="true"
        className={cx(
          'fixed left-1/2 top-1/2 z-[90] flex max-h-[calc(100dvh-32px)] w-[calc(100vw-32px)] -translate-x-1/2 -translate-y-1/2 flex-col overflow-hidden rounded-lg border border-slate-200 bg-white shadow-2xl shadow-slate-950/20',
          maxWidth,
        )}
        role="dialog"
      >
        <div className="flex items-start justify-between gap-3 border-b border-slate-100 bg-gradient-to-br from-white via-emerald-50/70 to-sky-50/60 p-4 sm:gap-4 sm:p-5">
          <div className="min-w-0">
            <p className="text-xs font-black uppercase text-emerald-700">{eyebrow}</p>
            <h2 className="mt-1 break-words text-lg font-black leading-tight text-slate-950 sm:text-xl">{title}</h2>
            {description ? <p className="mt-2 max-w-2xl text-sm font-semibold leading-6 text-slate-600">{description}</p> : null}
          </div>
          <IconButton label="Đóng" onClick={onClose}>
            <X size={18} />
          </IconButton>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto p-4 sm:p-5">{children}</div>
      </section>
    </>
  )
}

function ProfileDrawer({
  editTarget,
  form,
  loading,
  onAvatarClear,
  onAvatarFile,
  onClose,
  onSubmit,
  open,
  relationForm,
  setRelationForm,
  setField,
  uploadingAvatar,
}) {
  const isEdit = Boolean(editTarget)
  const editableProfileFields = canEditProfileFields(editTarget)
  const editableRelation = canEditProfileRelation(editTarget)
  const submitLabel = !isEdit ? 'Tạo hồ sơ' : editableProfileFields ? 'Lưu hồ sơ' : 'Lưu quan hệ'

  return (
    <CenteredCardShell
      description={
        isEdit
          ? editableProfileFields
            ? 'Cập nhật thông tin nhận diện, liên hệ, ảnh đại diện và quan hệ gia đình cho hồ sơ đang quản lý.'
            : 'Cập nhật quan hệ gia đình với hồ sơ người thân đã kết nối.'
          : 'Tạo hồ sơ người thân mới trong tài khoản chăm sóc của bạn.'
      }
      eyebrow="Hồ sơ người thân"
      open={open}
      title={isEdit ? 'Chỉnh sửa hồ sơ' : 'Tạo hồ sơ mới'}
      onClose={onClose}
    >
      <form className="grid gap-5" onSubmit={onSubmit}>
        <AvatarPicker
          disabled={loading || !editableProfileFields}
          profile={editTarget}
          uploading={uploadingAvatar}
          value={form.avatarUrl}
          onClear={onAvatarClear}
          onFile={onAvatarFile}
        />

        <div className="grid gap-4 lg:grid-cols-2">
          <div className="grid gap-4 rounded-lg border border-slate-100 bg-slate-50/70 p-4">
            <div>
              <p className="text-xs font-black uppercase text-emerald-700">Thông tin cơ bản</p>
              <h3 className="mt-1 text-base font-black text-slate-950">Tên và ngày sinh</h3>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Họ">
                <input
                  className={inputClass}
                  disabled={!editableProfileFields || loading}
                  value={form.firstName}
                  onChange={setField('firstName')}
                />
              </Field>
              <Field label="Tên">
                <input
                  className={inputClass}
                  disabled={!editableProfileFields || loading}
                  value={form.lastName}
                  onChange={setField('lastName')}
                />
              </Field>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Ngày sinh">
                <input
                  className={inputClass}
                  disabled={!editableProfileFields || loading}
                  type="date"
                  value={form.dateOfBirth}
                  onChange={setField('dateOfBirth')}
                />
              </Field>
              <Field label="Giới tính">
                <select
                  className={inputClass}
                  disabled={!editableProfileFields || loading}
                  value={form.gender}
                  onChange={setField('gender')}
                >
                  <option value="MALE">Nam</option>
                  <option value="FEMALE">Nữ</option>
                  <option value="OTHER">Khác</option>
                </select>
              </Field>
            </div>
          </div>

          <div className="grid gap-4 rounded-lg border border-slate-100 bg-white p-4">
            <div>
              <p className="text-xs font-black uppercase text-sky-700">Liên hệ chăm sóc</p>
              <h3 className="mt-1 text-base font-black text-slate-950">Số điện thoại và địa chỉ</h3>
            </div>
            <Field label="Số điện thoại">
              <input
                className={inputClass}
                disabled={!editableProfileFields || loading}
                value={form.phone}
                onChange={setField('phone')}
              />
            </Field>
            <Field label="Địa chỉ">
              <textarea
                className={textareaClass}
                disabled={!editableProfileFields || loading}
                value={form.address}
                onChange={setField('address')}
              />
            </Field>
          </div>
        </div>

        {editableRelation ? (
          <div className="grid gap-4 rounded-lg border border-amber-100 bg-amber-50/70 p-4">
            <div>
              <p className="text-xs font-black uppercase text-amber-700">Quan hệ gia đình</p>
              <h3 className="mt-1 text-base font-black text-slate-950">Cách gọi người thân</h3>
            </div>
            <RelationFields
              disabled={loading}
              customRelation={relationForm.customRelation}
              relation={relationForm.relation}
              onCustomRelationChange={(value) => setRelationForm((current) => ({ ...current, customRelation: value }))}
              onRelationChange={(value) =>
                setRelationForm((current) => ({
                  ...current,
                  relation: value,
                  customRelation: value === 'OTHER' ? current.customRelation : '',
                }))
              }
            />
          </div>
        ) : null}

        <div className="flex flex-col-reverse gap-3 border-t border-slate-100 pt-4 sm:flex-row sm:justify-end">
          <AppButton tone="ghost" onClick={onClose}>
            Hủy
          </AppButton>
          <AppButton disabled={loading || uploadingAvatar} type="submit">
            {loading ? <LoaderCircle className="animate-spin" size={16} /> : <Check size={16} />}
            {submitLabel}
          </AppButton>
        </div>
      </form>
    </CenteredCardShell>
  )
}

function RelationshipCard({ profile }) {
  const navigate = useNavigate()
  const handleChat = () => {
    const targetProfileId = profile?.profileId || profile?.id
    if (targetProfileId) {
      navigate(`/chat?profileId=${targetProfileId}`)
    }
  }

  return (
    <article className="grid gap-4 rounded-lg border border-slate-200 bg-white p-4 shadow-sm shadow-slate-100 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
      <div className="flex min-w-0 items-start gap-3">
        <ProfileBadge profile={profile} />
        <div className="min-w-0">
          <strong className="block truncate text-sm font-black text-slate-950">{fullName(profile)}</strong>
          <span className="mt-1 block truncate text-sm font-bold text-slate-600">
            {profile.phone || profile.address || 'Chưa cập nhật thông tin liên hệ'}
          </span>
          {relationshipLabel(profile) ? (
            <span className="mt-1 block text-xs font-bold text-slate-500">{relationshipLabel(profile)}</span>
          ) : null}
        </div>
      </div>
      <div className="flex items-center gap-2">
        <AppButton size="sm" tone="ghost" onClick={handleChat}>
          <MessageCircle size={14} />
          Trò chuyện
        </AppButton>
        <RelationChip profile={profile} />
      </div>
    </article>
  )
}

function RelationshipQuickActions({ profile, onNavigate }) {
  const hasProfile = Boolean(profile)
  const phoneHref = profile?.phone ? `tel:${profile.phone}` : undefined

  return (
    <div className="grid grid-cols-2 gap-2 md:grid-cols-4">
      <AppButton disabled={!hasProfile} tone="secondary" onClick={() => onNavigate('/medications', 'add')}>
        <Pill size={16} />
        Thêm thuốc
      </AppButton>
      <AppButton disabled={!hasProfile} tone="ghost" onClick={() => onNavigate('/dashboard', 'schedule')}>
        <CalendarClock size={16} />
        Sửa lịch
      </AppButton>
      <AppButton disabled={!hasProfile} tone="ghost" onClick={() => onNavigate('/chat')}>
        <MessageCircle size={16} />
        Chat
      </AppButton>
      <a
        aria-disabled={!phoneHref}
        className={cx(
          'inline-flex min-h-10 items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-4 text-sm font-black text-slate-700 transition',
          'hover:border-emerald-200 hover:bg-emerald-50 hover:text-emerald-800 focus:outline-none focus:ring-4 focus:ring-emerald-100',
          !phoneHref && 'pointer-events-none cursor-not-allowed opacity-60',
        )}
        href={phoneHref}
        onClick={(event) => {
          if (!phoneHref) {
            event.preventDefault()
          }
        }}
      >
        <Phone size={16} />
        Gọi
      </a>
    </div>
  )
}

function RelationshipTimeline({ profile }) {
  if (!profile) {
    return <EmptyState>Chọn một người thân để xem sự kiện và ghi chú gần đây.</EmptyState>
  }

  const items = [
    {
      body: profile.status === 'PENDING' ? 'Lời mời đã gửi và đang chờ phản hồi.' : 'Kết nối đã sẵn sàng cho các nghiệp vụ chăm sóc.',
      icon: ShieldCheck,
      meta: 'Trạng thái',
      title: statusLabel(profile.status),
      tone: 'emerald',
    },
    {
      body: 'Cách gọi này giúp phân biệt từng người thân trong danh sách chăm sóc.',
      icon: HeartHandshake,
      meta: 'Quan hệ',
      title: relationshipLabel(profile),
      tone: 'sky',
    },
    {
      body: 'Chưa có sự kiện mới từ dữ liệu backend. Khu vực này đã sẵn sàng để hiển thị khi API bổ sung.',
      icon: CalendarClock,
      meta: 'Sự kiện gần đây',
      title: 'Chưa có sự kiện mới',
      tone: 'amber',
    },
    {
      body: 'Chưa có ghi chú mới từ dữ liệu backend. Có thể kết nối phần ghi chú sau khi backend có endpoint.',
      icon: NotebookText,
      meta: 'Ghi chú gần đây',
      title: 'Chưa có ghi chú mới',
      tone: 'violet',
    },
  ]
  const tones = {
    amber: 'border-amber-100 bg-amber-50 text-amber-700',
    emerald: 'border-emerald-100 bg-emerald-50 text-emerald-700',
    sky: 'border-sky-100 bg-sky-50 text-sky-700',
    violet: 'border-violet-100 bg-violet-50 text-violet-700',
  }

  return (
    <div className="grid gap-3">
      {items.map((item) => {
        const Icon = item.icon

        return (
          <article className="grid grid-cols-[auto_1fr] gap-3 rounded-lg border border-slate-100 bg-slate-50/70 p-3" key={item.meta}>
            <span className={cx('grid h-9 w-9 place-items-center rounded-lg border', tones[item.tone])}>
              <Icon size={16} />
            </span>
            <div>
              <small className="text-xs font-black uppercase text-slate-500">{item.meta}</small>
              <strong className="mt-1 block text-sm font-black text-slate-950">{item.title}</strong>
              <p className="mt-1 text-sm font-semibold text-slate-600">{item.body}</p>
            </div>
          </article>
        )
      })}
    </div>
  )
}

function RelationshipOverview({ profile, onNavigate }) {
  return (
    <article className={cx(panelClass, 'grid content-start gap-4 p-4 sm:gap-5 sm:p-5')}>
      <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-start">
        <div className="flex min-w-0 items-start gap-3">
          <ProfileBadge profile={profile} size="lg" />
          <div className="min-w-0">
            <p className="text-xs font-black uppercase text-emerald-700">Tổng quan người thân</p>
            <h2 className="mt-1 break-words text-xl font-black leading-tight text-slate-950 sm:text-2xl">{profile ? fullName(profile) : 'Chưa chọn hồ sơ'}</h2>
            <span className="mt-1 block text-sm font-bold text-slate-500">{profile ? relationshipLabel(profile) : 'Chưa đặt quan hệ'}</span>
          </div>
        </div>
        {profile ? <StatusPill status={profile.status} /> : null}
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="grid gap-2 rounded-lg bg-emerald-50 p-3 text-sm font-bold text-emerald-900">
          <Phone size={16} />
          <span className="break-words">{profile?.phone || 'Chưa cập nhật số điện thoại'}</span>
        </div>
        <div className="grid gap-2 rounded-lg bg-sky-50 p-3 text-sm font-bold text-sky-900">
          <MapPin size={16} />
          <span className="break-words">{profile?.address || 'Chưa cập nhật địa chỉ'}</span>
        </div>
        <div className="grid gap-2 rounded-lg bg-amber-50 p-3 text-sm font-bold text-amber-900">
          <HeartHandshake size={16} />
          <span>{profile ? relationshipLabel(profile) : 'Chưa đặt quan hệ'}</span>
        </div>
      </div>

      <RelationshipQuickActions profile={profile} onNavigate={onNavigate} />

      <div>
        <p className="text-xs font-black uppercase text-emerald-700">Theo dõi gần đây</p>
        <h3 className="mt-1 text-base font-black text-slate-950">Sự kiện và ghi chú</h3>
      </div>
      <RelationshipTimeline profile={profile} />
    </article>
  )
}

function DetailInfoRow({ icon: Icon, label, value }) {
  return (
    <div className="grid gap-2 rounded-lg border border-slate-100 bg-slate-50/80 p-3">
      <span className="inline-flex items-center gap-2 text-xs font-black uppercase text-slate-500">
        <Icon size={15} />
        {label}
      </span>
      <strong className="break-words text-sm font-black text-slate-950">{value || 'Chưa cập nhật'}</strong>
    </div>
  )
}

function ProfileDetailCard({ onClose, onDelete, onEdit, open, profile }) {
  return (
    <CenteredCardShell
      description="Xem nhanh thông tin liên hệ, quan hệ gia đình và trạng thái kết nối của hồ sơ được chọn."
      eyebrow="Chi tiết hồ sơ"
      maxWidth="max-w-3xl"
      open={open}
      title={profile ? fullName(profile) : 'Chi tiết hồ sơ'}
      onClose={onClose}
    >
      {profile ? (
        <div className="grid gap-5">
          <div className="grid gap-4 rounded-lg border border-emerald-100 bg-emerald-50/70 p-4 sm:grid-cols-[auto_minmax(0,1fr)_auto] sm:items-center">
            <ProfileBadge profile={profile} size="xl" />
            <div className="min-w-0">
              <p className="text-xs font-black uppercase text-emerald-700">Hồ sơ người thân</p>
              <h3 className="mt-1 break-words text-2xl font-black leading-tight text-slate-950">{fullName(profile)}</h3>
              <span className="mt-2 inline-flex rounded-lg border border-white/80 bg-white px-3 py-1 text-sm font-black text-emerald-800">
                {relationshipLabel(profile)}
              </span>
            </div>
            <StatusPill status={profile.status} />
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <DetailInfoRow icon={Phone} label="Số điện thoại" value={profile.phone} />
            <DetailInfoRow icon={CalendarClock} label="Ngày sinh" value={profile.dateOfBirth} />
            <DetailInfoRow icon={UserRound} label="Giới tính" value={GENDER_LABELS[profile.gender]} />
            <DetailInfoRow icon={MapPin} label="Địa chỉ" value={profile.address} />
          </div>

          <div className="rounded-lg border border-slate-100 bg-white p-4">
            <p className="text-xs font-black uppercase text-slate-500">Trạng thái chăm sóc</p>
            <p className="mt-2 text-sm font-semibold leading-6 text-slate-600">
              {profile.status === 'PENDING'
                ? 'Lời mời đang chờ người thân phản hồi. Bạn có thể theo dõi trạng thái trong danh sách kết nối.'
                : 'Hồ sơ đã sẵn sàng cho các thao tác chăm sóc như thuốc, lịch uống, chat và gọi điện.'}
            </p>
          </div>

          <div className="flex flex-col gap-3 border-t border-slate-100 pt-4 sm:flex-row sm:justify-end">
            <AppButton onClick={() => onEdit(profile)}>
              <Pencil size={16} />
              Sửa hồ sơ
            </AppButton>
            <AppButton tone="danger" onClick={() => onDelete(profile)}>
              <Trash2 size={16} />
              Xóa hồ sơ
            </AppButton>
            <AppButton tone="ghost" onClick={onClose}>
              Đóng
            </AppButton>
          </div>
        </div>
      ) : null}
    </CenteredCardShell>
  )
}

function RelationshipTable({ isLoading, onView, relationships, selectedKey }) {
  if (!isLoading && !relationships.length) {
    return <EmptyState>Không có người thân trong trạng thái này.</EmptyState>
  }

  return (
    <>
      <div className="grid gap-3 xl:hidden">
        {isLoading
          ? Array.from({ length: 4 }).map((_, index) => (
              <RelationshipMobileCard isLoading key={`relationship-mobile-skeleton-${index}`} />
            ))
          : relationships.map((profile) => {
              const key = relationshipKey(profile)

              return (
                <RelationshipMobileCard
                  isSelected={key === selectedKey}
                  key={profile.relationshipId ?? key}
                  profile={profile}
                  onView={onView}
                />
              )
            })}
      </div>

      <div className="hidden overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm shadow-slate-100 xl:block">
        <table className="w-full table-fixed border-collapse text-left">
          <colgroup>
            <col className="w-[32%]" />
            <col className="w-[28%]" />
            <col className="w-[18%]" />
            <col className="w-[14%]" />
            <col className="w-[8%]" />
          </colgroup>
          <thead className="border-b border-slate-100 bg-slate-50/90 text-xs font-black uppercase text-slate-500">
            <tr>
              <th className="px-5 py-3.5">Người thân</th>
              <th className="px-5 py-3.5">Liên hệ</th>
              <th className="px-5 py-3.5">Quan hệ</th>
              <th className="px-5 py-3.5">Trạng thái</th>
              <th className="px-5 py-3.5 text-right">Mở</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading ? (
              Array.from({ length: 4 }).map((_, index) => (
                <tr key={`relationship-skeleton-${index}`}>
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-3">
                      <SkeletonBlock className="h-11 w-11 rounded-xl" />
                      <div className="grid min-w-0 flex-1 gap-2">
                        <SkeletonBlock className="h-4 w-40 max-w-full" />
                        <SkeletonBlock className="h-3 w-28 max-w-full" />
                      </div>
                    </div>
                  </td>
                  <td className="px-5 py-4">
                    <div className="grid gap-2">
                      <SkeletonBlock className="h-3.5 w-36 max-w-full" />
                      <SkeletonBlock className="h-3.5 w-48 max-w-full" />
                    </div>
                  </td>
                  <td className="px-5 py-4">
                    <SkeletonBlock className="h-8 w-24" />
                  </td>
                  <td className="px-5 py-4">
                    <SkeletonBlock className="h-8 w-24" />
                  </td>
                  <td className="px-5 py-4">
                    <div className="flex justify-end">
                      <SkeletonBlock className="h-9 w-9" />
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              relationships.map((profile) => {
                const key = relationshipKey(profile)
                const isSelected = key === selectedKey

                return (
                  <tr
                    className={cx('cursor-pointer align-top transition hover:bg-emerald-50/50', isSelected && 'bg-emerald-50/70')}
                    key={profile.relationshipId ?? key}
                    onClick={() => onView(profile)}
                  >
                    <td className="px-5 py-4">
                      <button
                        className="flex min-w-0 items-center gap-3 text-left"
                        type="button"
                        onClick={(event) => {
                          event.stopPropagation()
                          onView(profile)
                        }}
                      >
                        <ProfileBadge profile={profile} size="sm" />
                        <span className="min-w-0">
                          <strong className="block truncate text-sm font-black text-slate-950">{fullName(profile)}</strong>
                          <small className="mt-1 block truncate text-xs font-bold text-slate-500">
                            {GENDER_LABELS[profile.gender] || 'Chưa cập nhật'} · {profile.dateOfBirth || 'Chưa có ngày sinh'}
                          </small>
                        </span>
                      </button>
                    </td>
                    <td className="px-5 py-4">
                      <div className="grid gap-2 text-slate-700">
                        <ContactLine icon={Phone}>
                          <span className="truncate">{profile.phone || 'Chưa cập nhật số điện thoại'}</span>
                        </ContactLine>
                        <ContactLine icon={MapPin} className="text-slate-500">
                          <span className="line-clamp-2">{profile.address || 'Chưa cập nhật địa chỉ'}</span>
                        </ContactLine>
                      </div>
                    </td>
                    <td className="px-5 py-4">
                      <RelationChip profile={profile} />
                    </td>
                    <td className="px-5 py-4">
                      <StatusPill status={profile.status} />
                    </td>
                    <td className="px-5 py-4 text-right">
                      <div onClick={(event) => event.stopPropagation()}>
                        <IconActionButton label={`Mở chi tiết ${fullName(profile)}`} onClick={() => onView(profile)}>
                          <Eye size={16} />
                        </IconActionButton>
                      </div>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>
    </>
  )
}

function InviteDrawer({
  acceptedProfileIds,
  invitedProfileIds,
  inviteForm,
  inviteTarget,
  invitingId,
  onClose,
  onInvite,
  onSearch,
  open,
  pendingProfileIds,
  searchQuery,
  searchResults,
  searching,
  setInviteField,
  setInviteForm,
  setInviteTarget,
  setSearchQuery,
}) {
  function invitationState(profile) {
    const key = profile.id ?? profile.profileId

    if (acceptedProfileIds.has(key)) {
      return 'accepted'
    }

    if (pendingProfileIds.has(key) || invitedProfileIds.has(key)) {
      return 'pending'
    }

    return 'available'
  }

  const targetState = inviteTarget ? invitationState(inviteTarget) : 'available'

  return (
    <CenteredCardShell
      description="Tìm hồ sơ người thân đã có trong hệ thống, chọn đúng người và gửi lời mời kết nối chăm sóc."
      eyebrow="Lời mời mới"
      maxWidth="max-w-5xl"
      open={open}
      title="Mời người thân mới"
      onClose={onClose}
    >
      <div className="grid gap-5 lg:grid-cols-[minmax(0,1.15fr)_minmax(320px,0.85fr)]">
        <section className="grid gap-4 rounded-lg border border-slate-100 bg-slate-50/70 p-4">
          <form className="grid gap-3" onSubmit={onSearch}>
          <Field label="Tìm theo tên, số điện thoại hoặc email">
            <div className="grid gap-2 sm:grid-cols-[minmax(0,1fr)_auto]">
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={17} />
                <input
                  className={cx(inputClass, 'pl-10')}
                  placeholder="Nhập tên, số điện thoại hoặc email"
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                />
              </div>
              <AppButton disabled={searching} type="submit">
                {searching ? <LoaderCircle className="animate-spin" size={16} /> : <Search size={16} />}
                Tìm
              </AppButton>
            </div>
          </Field>
        </form>

          <div className="grid gap-3">
          <div>
            <p className="text-xs font-black uppercase text-emerald-700">Kết quả tìm kiếm</p>
            <h3 className="mt-1 text-base font-black text-slate-950">Chọn hồ sơ muốn mời</h3>
          </div>
          {searchResults.length ? (
            searchResults.map((profile) => {
              const key = profile.id ?? profile.profileId
              const state = invitationState(profile)
              const isSelected = (inviteTarget?.id ?? inviteTarget?.profileId) === key

              return (
                <article
                  className={cx(
                    'grid gap-3 rounded-lg border p-3 transition sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center',
                    isSelected ? 'border-emerald-300 bg-emerald-50' : 'border-slate-200 bg-white hover:border-emerald-200 hover:bg-emerald-50/60',
                  )}
                  key={key}
                >
                  <div className="flex min-w-0 items-start gap-3">
                    <ProfileBadge profile={profile} />
                    <div className="min-w-0">
                      <strong className="block truncate text-sm font-black text-slate-950">{fullName(profile)}</strong>
                      <span className="mt-1 block truncate text-sm font-bold text-slate-600">{profile.phone || 'Chưa cập nhật số điện thoại'}</span>
                      <span className="mt-1 inline-flex rounded-full border border-slate-200 bg-slate-50 px-2 py-1 text-xs font-black text-slate-600">
                        {PROFILE_ROLE_LABELS[profile.role] ?? 'Người thân'}
                      </span>
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-2 sm:justify-end">
                    {state === 'accepted' ? <StatusPill status="ACCEPTED" /> : null}
                    {state === 'pending' ? <StatusPill status="PENDING" /> : null}
                    <AppButton
                      disabled={state !== 'available'}
                      size="sm"
                      tone={isSelected ? 'primary' : 'secondary'}
                      onClick={() => {
                        setInviteTarget(profile)
                        setInviteForm(EMPTY_INVITE_FORM)
                      }}
                    >
                      {isSelected ? <Check size={14} /> : <Plus size={14} />}
                      {isSelected ? 'Đã chọn' : 'Chọn'}
                    </AppButton>
                  </div>
                </article>
              )
            })
          ) : (
            <EmptyState>Nhập từ khóa để tìm hồ sơ người thân có sẵn trong hệ thống.</EmptyState>
          )}
          </div>
        </section>

        <form className="grid content-start gap-4 rounded-lg border border-emerald-100 bg-white p-4" onSubmit={onInvite}>
          <div className="flex items-start gap-3 rounded-lg border border-emerald-100 bg-emerald-50 p-4">
            <ProfileBadge profile={inviteTarget} />
            <div className="min-w-0">
              <p className="text-xs font-black uppercase text-emerald-700">Hồ sơ được mời</p>
              <strong className="mt-1 block truncate text-sm font-black text-slate-950">
                {inviteTarget ? fullName(inviteTarget) : 'Chưa chọn hồ sơ'}
              </strong>
              <span className="mt-1 block truncate text-xs font-bold text-slate-600">
                {inviteTarget?.phone || 'Chọn một kết quả tìm kiếm để gửi lời mời'}
              </span>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Cách người thân gọi bạn">
              <input className={inputClass} value={inviteForm.caregiverTitle} onChange={setInviteField('caregiverTitle')} />
            </Field>
            <RelationFields
              customRelation={inviteForm.customRelation}
              relation={inviteForm.relation}
              onCustomRelationChange={(value) => setInviteForm((current) => ({ ...current, customRelation: value }))}
              onRelationChange={(value) => setInviteForm((current) => ({ ...current, relation: value, customRelation: value === 'OTHER' ? current.customRelation : '' }))}
            />
          </div>

          <div className="rounded-lg bg-sky-50 p-3 text-sm font-semibold leading-6 text-sky-900">
            Người thân cần chấp nhận lời mời trước khi bạn có thể xem lịch thuốc hoặc nhắn tin trong hồ sơ đó.
          </div>

          <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <AppButton tone="ghost" onClick={onClose}>
              Hủy
            </AppButton>
            <AppButton disabled={!inviteTarget || targetState !== 'available' || Boolean(invitingId)} type="submit">
              {invitingId ? <LoaderCircle className="animate-spin" size={16} /> : <Send size={16} />}
              {targetState === 'pending' ? 'Đang chờ phản hồi' : 'Gửi lời mời'}
            </AppButton>
          </div>
        </form>
      </div>
    </CenteredCardShell>
  )
}

function ConfirmDeleteDialog({ target, onCancel, onConfirm }) {
  if (!target) {
    return null
  }

  const isLocalProfile = isManagedLocalProfile(target)

  return (
    <>
      <div className="fixed inset-0 z-[100] bg-slate-950/45 backdrop-blur-sm" onClick={onCancel} />
      <div
        aria-modal="true"
        className="fixed left-1/2 top-1/2 z-[110] grid w-[calc(100vw-32px)] max-w-md -translate-x-1/2 -translate-y-1/2 gap-4 rounded-lg border border-slate-200 bg-white p-5 shadow-2xl"
        role="dialog"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-black uppercase text-rose-700">Xóa hồ sơ</p>
            <h2 className="mt-1 text-xl font-black text-slate-950">Xóa hồ sơ người thân</h2>
          </div>
          <IconButton label="Đóng" onClick={onCancel}>
            <X size={18} />
          </IconButton>
        </div>
        <p className="text-sm font-semibold text-slate-600">
          {isLocalProfile
            ? `Bạn có chắc muốn xóa hồ sơ “${fullName(target)}”? Thao tác này chỉ áp dụng với hồ sơ trong tài khoản của bạn.`
            : `Bạn có chắc muốn xóa kết nối với “${fullName(target)}”? Hồ sơ gốc của người thân sẽ không bị xóa khỏi hệ thống.`}
        </p>
        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <AppButton tone="ghost" onClick={onCancel}>
            Hủy
          </AppButton>
          <AppButton tone="danger" onClick={onConfirm}>
            <Trash2 size={16} />
            Xóa hồ sơ
          </AppButton>
        </div>
      </div>
    </>
  )
}

function CaregiverRelationshipsPage() {
  const navigate = useNavigate()
  const replaceProfiles = useAuthStore((state) => state.replaceProfiles)
  const [localProfiles, setLocalProfiles] = useState([])
  const [acceptedRelationships, setAcceptedRelationships] = useState([])
  const [pendingRelationships, setPendingRelationships] = useState([])
  const [relationshipFilter, setRelationshipFilter] = useState('ACCEPTED')
  const [selectedRelationshipId, setSelectedRelationshipId] = useState(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [inviteForm, setInviteForm] = useState(EMPTY_INVITE_FORM)
  const [inviteDrawerOpen, setInviteDrawerOpen] = useState(false)
  const [inviteTarget, setInviteTarget] = useState(null)
  const [invitedProfileIds, setInvitedProfileIds] = useState(() => new Set())
  const [invitingId, setInvitingId] = useState(null)
  const [form, setForm] = useState(EMPTY_PROFILE_FORM)
  const [editTarget, setEditTarget] = useState(null)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [relationForm, setRelationForm] = useState({ relation: 'FATHER', customRelation: '' })
  const [detailTarget, setDetailTarget] = useState(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [searching, setSearching] = useState(false)
  const [uploadingAvatar, setUploadingAvatar] = useState(false)
  const caregiverLoadRequestRef = useRef(0)
  const hasLoadedCaregiverDataRef = useRef(false)

  const loadCaregiverData = useCallback(async () => {
    const requestId = caregiverLoadRequestRef.current + 1
    caregiverLoadRequestRef.current = requestId

    const isInitialLoad = !hasLoadedCaregiverDataRef.current
    setLoading(isInitialLoad)
    setRefreshing(!isInitialLoad)
    try {
      const [profilesPage, accepted, pending] = await Promise.all([
        getProfiles({ page: 0, size: 100 }),
        getCaregiverRelationships(),
        getPendingCaregiverRelationships(),
      ])

      if (caregiverLoadRequestRef.current !== requestId) {
        return
      }

      const accountProfiles = asPageContent(profilesPage)
      const relativeProfiles = accountProfiles.filter((profile) => profile.role === 'ELDERLY')
      setLocalProfiles(relativeProfiles)
      setAcceptedRelationships(Array.isArray(accepted) ? accepted : [])
      setPendingRelationships(Array.isArray(pending) ? pending : [])
      replaceProfiles([useAuthStore.getState().activeProfile, ...accountProfiles].filter(Boolean))
    } catch (err) {
      if (caregiverLoadRequestRef.current === requestId) {
        notify.apiError(err, 'Không thể tải danh sách người thân')
      }
    } finally {
      if (caregiverLoadRequestRef.current === requestId) {
        hasLoadedCaregiverDataRef.current = true
        setLoading(false)
        setRefreshing(false)
      }
    }
  }, [replaceProfiles])

  useEffect(() => {
    loadCaregiverData()
  }, [loadCaregiverData])

  const setProfileField = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }))
  }

  const setInviteField = (field) => (event) => {
    setInviteForm((current) => ({ ...current, [field]: event.target.value }))
  }

  async function handleAvatarFile(file) {
    setUploadingAvatar(true)
    try {
      const compressed = await compressImage(file, { maxWidth: 720, maxHeight: 720, quality: 0.82 })
      const avatarUrl = await uploadImageToCloudinary(compressed, 'avatar')
      setForm((current) => ({ ...current, avatarUrl }))
      notify.success('Đã tải ảnh đại diện')
    } catch (err) {
      notify.apiError(err, 'Không thể tải ảnh đại diện')
    } finally {
      setUploadingAvatar(false)
    }
  }

  function clearAvatar() {
    setForm((current) => ({ ...current, avatarUrl: '' }))
  }

  function openCreateDrawer() {
    setEditTarget(null)
    setDetailTarget(null)
    setForm(EMPTY_PROFILE_FORM)
    setRelationForm({ relation: 'FATHER', customRelation: '' })
    setDrawerOpen(true)
  }

  function openInviteDrawer() {
    setDetailTarget(null)
    setInviteTarget(null)
    setSearchQuery('')
    setSearchResults([])
    setInviteForm(EMPTY_INVITE_FORM)
    setInviteDrawerOpen(true)
  }

  async function openEditDrawer(profile) {
    setEditTarget(profile)
    setDetailTarget(null)
    setDrawerOpen(true)
    setForm(profileToForm(profile))
    setRelationForm(relationFormFromProfile(profile))
    if (!isManagedLocalProfile(profile)) {
      return
    }

    try {
      const detail = await getManagedElderlyProfile(profile.id)
      setEditTarget({ ...detail, source: 'local' })
      setForm(profileToForm(detail))
      setRelationForm(relationFormFromProfile(detail))
    } catch (err) {
      notify.apiError(err, 'Không thể tải chi tiết hồ sơ')
    }
  }

  function validateProfileForm() {
    if (!form.firstName.trim() || !form.lastName.trim() || !form.phone.trim() || !form.dateOfBirth || !form.gender) {
      notify.warning('Vui lòng nhập đầy đủ họ tên, số điện thoại, ngày sinh và giới tính')
      return false
    }
    return true
  }

  async function handleSaveProfile(event) {
    event.preventDefault()
    const shouldSaveProfile = !editTarget || isManagedLocalProfile(editTarget)
    const shouldSaveLinkedRelation = Boolean(editTarget && !isManagedLocalProfile(editTarget))

    if (shouldSaveProfile && uploadingAvatar) {
      notify.warning('Vui lòng chờ ảnh đại diện tải xong')
      return
    }
    if (shouldSaveProfile && !validateProfileForm()) {
      return
    }

    setSaving(true)
    try {
      if (editTarget?.id && shouldSaveProfile) {
        await updateManagedElderlyProfile(editTarget.id, buildProfilePayload(form, relationForm))
        notify.success('Đã cập nhật hồ sơ người thân')
      } else if (!editTarget) {
        await createManagedElderlyProfile(buildProfilePayload(form, relationForm))
        notify.success('Đã tạo hồ sơ người thân')
      }
      if (shouldSaveLinkedRelation) {
        const targetId = relationshipKey(editTarget)
        const nextRelation = relationPayload(relationForm)
        await updateCaregiverRelationship(targetId, nextRelation)
        if ((editTarget?.status || '').toUpperCase() === 'PENDING') {
          setPendingRelationships((current) =>
            current.map((profile) => (profileKey(profile) === targetId ? applyRelationPayload(profile, nextRelation) : profile)),
          )
        } else {
          setAcceptedRelationships((current) =>
            current.map((profile) => (profileKey(profile) === targetId ? applyRelationPayload(profile, nextRelation) : profile)),
          )
          setPendingRelationships((current) => current.filter((profile) => profileKey(profile) !== targetId))
        }
        setSelectedRelationshipId(targetId)
        notify.success('Đã cập nhật quan hệ')
      }
      setDrawerOpen(false)
      await loadCaregiverData()
    } catch (err) {
      notify.apiError(err, 'Không thể lưu hồ sơ người thân')
    } finally {
      setSaving(false)
    }
  }

  async function handleDeleteProfile() {
    if (!deleteTarget) {
      return
    }

    try {
      if (isManagedLocalProfile(deleteTarget)) {
        await deleteManagedElderlyProfile(deleteTarget.id)
        notify.success('Đã xóa hồ sơ người thân')
      } else {
        await deleteCaregiverRelationship(relationshipKey(deleteTarget))
        notify.success('Đã xóa kết nối người thân')
      }
      await loadCaregiverData()
    } catch (err) {
      notify.apiError(err, 'Không thể xóa hồ sơ')
    } finally {
      setDeleteTarget(null)
    }
  }

  async function handleSearch(event) {
    event.preventDefault()
    if (!searchQuery.trim()) {
      notify.warning('Nhập tên, số điện thoại hoặc email để tìm người thân')
      return
    }

    setSearching(true)
    try {
      const result = await searchElderlyProfiles({ query: searchQuery.trim(), page: 0, size: 8 })
      setSearchResults(asPageContent(result))
    } catch (err) {
      notify.apiError(err, 'Không thể tìm hồ sơ người thân')
    } finally {
      setSearching(false)
    }
  }

  async function handleInvite(event) {
    event.preventDefault()
    if (!inviteTarget) {
      notify.warning('Chọn một hồ sơ trước khi gửi lời mời')
      return
    }

    const targetId = inviteTarget.id ?? inviteTarget.profileId
    setInvitingId(targetId)
    try {
      await inviteElderlyProfile({
        targetElderlyId: targetId,
        caregiverTitle: normalizeOptional(inviteForm.caregiverTitle),
        ...relationPayload(inviteForm),
      })
      setInvitedProfileIds((current) => {
        const next = new Set(current)
        next.add(targetId)
        return next
      })
      notify.success('Đã gửi lời mời')
      await loadCaregiverData()
    } catch (err) {
      notify.apiError(err, 'Không thể gửi lời mời')
    } finally {
      setInvitingId(null)
    }
  }

  function handleNavigate(path, action, profile) {
    const targetProfile = profile ?? selectedProfile
    navigate(`${path}${profileQuery(targetProfile, action)}`)
  }

  function handleViewRelationship(profile) {
    setSelectedRelationshipId(relationshipKey(profile))
    setDetailTarget(profile)
  }

  function handleEditFromDetail(profile) {
    setDetailTarget(null)
    openEditDrawer(profile)
  }

  function handleDeleteFromDetail(profile) {
    setDetailTarget(null)
    setDeleteTarget(profile)
  }

  const allRelationships = useMemo(() => {
    const acceptedProfiles = acceptedRelationships.map((profile) => normalizeRelationship(profile, 'ACCEPTED'))
    const acceptedProfileKeys = new Set(acceptedProfiles.map((profile) => profileKey(profile)).filter(Boolean))
    const pendingProfiles = pendingRelationships
      .map((profile) => normalizeRelationship(profile, 'PENDING'))
      .filter((profile) => !acceptedProfileKeys.has(profileKey(profile)))
    const relationProfilesById = new Map(
      [...pendingProfiles, ...acceptedProfiles]
        .map((profile) => [profileKey(profile), profile])
        .filter(([key]) => Boolean(key)),
    )
    const managedLocalProfiles = localProfiles.map((profile) => {
      const linkedRelation = relationProfilesById.get(profile.id)

      return normalizeRelationship(
        {
          ...profile,
          profileId: profile.id,
          elderlyTitle: linkedRelation?.elderlyTitle,
          relation: linkedRelation?.relation ?? profile.relation ?? 'OTHER',
          customRelation: linkedRelation?.customRelation ?? profile.customRelation,
          relationLabel: linkedRelation ? relationshipLabel(linkedRelation) : undefined,
          source: 'local',
        },
        'LOCAL',
      )
    })

    return [...acceptedProfiles, ...pendingProfiles, ...managedLocalProfiles]
  }, [acceptedRelationships, localProfiles, pendingRelationships])

  const relationshipCounts = useMemo(
    () =>
      RELATIONSHIP_FILTERS.reduce((counts, filter) => {
        counts[filter.value] = allRelationships.filter((profile) => profile.status === filter.value).length
        return counts
      }, {}),
    [allRelationships],
  )

  const filteredRelationships = useMemo(
    () => allRelationships.filter((profile) => profile.status === relationshipFilter),
    [allRelationships, relationshipFilter],
  )

  const acceptedProfileIds = useMemo(
    () => new Set(acceptedRelationships.map((item) => relationshipKey(item)).filter(Boolean)),
    [acceptedRelationships],
  )

  const pendingProfileIds = useMemo(
    () => new Set(pendingRelationships.map((item) => relationshipKey(item)).filter(Boolean)),
    [pendingRelationships],
  )

  useEffect(() => {
    const availableIds = allRelationships.map((profile) => relationshipKey(profile)).filter(Boolean)

    if (!availableIds.length) {
      if (selectedRelationshipId) {
        setSelectedRelationshipId(null)
      }
      return
    }

    if (!selectedRelationshipId || !availableIds.includes(selectedRelationshipId)) {
      setSelectedRelationshipId(availableIds[0])
    }
  }, [allRelationships, selectedRelationshipId])

  const selectedRelationship = allRelationships.find((profile) => relationshipKey(profile) === selectedRelationshipId)
  const selectedProfile = selectedRelationship ?? null

  return (
    <div className="mx-auto grid w-full max-w-[1480px] gap-4 sm:gap-5">
      <section className="overflow-hidden rounded-lg border border-emerald-100 bg-gradient-to-br from-sky-50 via-emerald-50 to-amber-50 p-4 shadow-lg shadow-slate-200/60 sm:p-5 lg:p-7">
        <div className="grid gap-5">
          <div className="max-w-4xl">
            <p className="text-xs font-black uppercase text-emerald-700">Không gian chăm sóc</p>
            <h1 className="mt-2 text-3xl font-black leading-tight text-slate-950 sm:text-4xl">Người thân của tôi</h1>
            <p className="mt-3 max-w-3xl text-sm font-semibold leading-6 text-slate-600 sm:text-base">
              Xem danh sách kết nối, cập nhật quan hệ gia đình và đi nhanh đến thuốc, lịch uống, chat hoặc gọi điện.
            </p>
          </div>
        </div>
      </section>

      <section className={cx(panelClass, 'grid content-start gap-4 p-4 sm:p-5')}>
        <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_auto] md:items-start">
          <div>
            <p className="text-xs font-black uppercase text-emerald-700">Danh sách kết nối</p>
            <h2 className="mt-1 text-xl font-black text-slate-950">Người thân theo trạng thái</h2>
          </div>
          <div className="grid gap-2 sm:grid-cols-2 md:flex md:flex-wrap md:items-center md:justify-end">
            {refreshing ? (
              <span className="inline-flex min-h-10 items-center justify-center gap-2 rounded-lg border border-emerald-100 bg-emerald-50 px-3 text-xs font-black text-emerald-700 sm:col-span-2 md:col-span-1" role="status">
                <LoaderCircle className="animate-spin" size={14} />
                Đang cập nhật
              </span>
            ) : null}
            <AppButton className="w-full md:w-auto" tone="secondary" onClick={openCreateDrawer}>
              <Plus size={16} />
              Tạo hồ sơ
            </AppButton>
            <AppButton className="w-full md:w-auto" onClick={openInviteDrawer}>
              <Send size={16} />
              Mời người thân mới
            </AppButton>
          </div>
        </div>

        <div className="grid gap-2 sm:grid-cols-3" role="tablist" aria-label="Lọc trạng thái người thân">
          {RELATIONSHIP_FILTERS.map((filter) => {
            const isActive = relationshipFilter === filter.value

            return (
              <button
                aria-selected={isActive}
                className={cx(
                  'flex min-h-12 items-center justify-between gap-3 rounded-lg border px-3 text-left transition',
                  isActive
                    ? 'border-emerald-300 bg-emerald-50 text-emerald-800'
                    : 'border-slate-200 bg-white text-slate-600 hover:border-sky-200 hover:bg-sky-50',
                )}
                key={filter.value}
                role="tab"
                type="button"
                onClick={() => setRelationshipFilter(filter.value)}
              >
                <span className="text-sm font-black">{filter.label}</span>
                <strong className="grid h-7 min-w-7 place-items-center rounded-full bg-white px-2 text-xs font-black shadow-sm">
                  {relationshipCounts[filter.value] ?? 0}
                </strong>
              </button>
            )
          })}
        </div>

        <RelationshipTable
          isLoading={loading}
          relationships={filteredRelationships}
          selectedKey={relationshipKey(selectedProfile)}
          onView={handleViewRelationship}
        />
      </section>

      <section>
        <RelationshipOverview profile={selectedProfile} onNavigate={handleNavigate} />
      </section>

      <ProfileDrawer
        editTarget={editTarget}
        form={form}
        loading={saving}
        open={drawerOpen}
        relationForm={relationForm}
        setRelationForm={setRelationForm}
        uploadingAvatar={uploadingAvatar}
        onAvatarClear={clearAvatar}
        onAvatarFile={handleAvatarFile}
        setField={setProfileField}
        onClose={() => setDrawerOpen(false)}
        onSubmit={handleSaveProfile}
      />

      <InviteDrawer
        acceptedProfileIds={acceptedProfileIds}
        invitedProfileIds={invitedProfileIds}
        inviteForm={inviteForm}
        inviteTarget={inviteTarget}
        invitingId={invitingId}
        open={inviteDrawerOpen}
        pendingProfileIds={pendingProfileIds}
        searchQuery={searchQuery}
        searchResults={searchResults}
        searching={searching}
        setInviteField={setInviteField}
        setInviteForm={setInviteForm}
        setInviteTarget={setInviteTarget}
        setSearchQuery={setSearchQuery}
        onClose={() => setInviteDrawerOpen(false)}
        onInvite={handleInvite}
        onSearch={handleSearch}
      />

      <ProfileDetailCard
        open={Boolean(detailTarget)}
        profile={detailTarget}
        onClose={() => setDetailTarget(null)}
        onDelete={handleDeleteFromDetail}
        onEdit={handleEditFromDetail}
      />

      <ConfirmDeleteDialog target={deleteTarget} onCancel={() => setDeleteTarget(null)} onConfirm={handleDeleteProfile} />
    </div>
  )
}

function ElderlyRelationshipsPage() {
  const [profile, setProfile] = useState(null)
  const [acceptedCaregivers, setAcceptedCaregivers] = useState([])
  const [pendingCaregivers, setPendingCaregivers] = useState([])
  const [loading, setLoading] = useState(true)

  const loadElderlyData = useCallback(async () => {
    setLoading(true)
    try {
      const [profileData, accepted, pending] = await Promise.all([
        getMyProfile(),
        getElderlyRelationships(),
        getPendingElderlyRelationships(),
      ])
      setProfile(profileData)
      setAcceptedCaregivers(Array.isArray(accepted) ? accepted : [])
      setPendingCaregivers(Array.isArray(pending) ? pending : [])
    } catch (err) {
      notify.apiError(err, 'Không thể tải danh sách người hỗ trợ')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadElderlyData()
  }, [loadElderlyData])

  async function handleAccept(relationshipId) {
    try {
      await acceptCaregiverInvitation(relationshipId)
      notify.success('Đã chấp nhận lời mời hỗ trợ')
      await loadElderlyData()
    } catch (err) {
      notify.apiError(err, 'Không thể chấp nhận lời mời')
    }
  }

  async function handleRefuse(relationshipId) {
    try {
      await refuseCaregiverInvitation(relationshipId)
      notify.success('Đã từ chối lời mời')
      await loadElderlyData()
    } catch (err) {
      notify.apiError(err, 'Không thể từ chối lời mời')
    }
  }

  if (loading) {
    return <LoadingState>Đang tải danh sách người hỗ trợ...</LoadingState>
  }

  return (
    <div className="mx-auto grid w-full max-w-[1480px] gap-4 sm:gap-5">
      <section className="overflow-hidden rounded-lg border border-sky-100 bg-gradient-to-br from-emerald-50 via-sky-50 to-violet-50 p-4 shadow-lg shadow-slate-200/60 sm:p-5 lg:p-7">
        <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-center">
          <div>
            <p className="text-xs font-black uppercase text-emerald-700">Hồ sơ cá nhân</p>
            <h1 className="mt-2 text-3xl font-black leading-tight text-slate-950 sm:text-4xl">Người hỗ trợ của tôi</h1>
            <p className="mt-3 max-w-3xl text-sm font-semibold leading-6 text-slate-600 sm:text-base">
              Xem thông tin cá nhân, danh sách người đã kết nối và xử lý các lời mời đang chờ xác nhận.
            </p>
          </div>
          <div className="inline-flex min-h-10 items-center gap-2 rounded-full border border-emerald-200 bg-white/80 px-4 text-sm font-black text-emerald-800">
            <UserRound size={16} />
            Chỉ xem hồ sơ
          </div>
        </div>
      </section>

      <section className="grid gap-5 lg:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)]">
        <article className={cx(panelClass, 'grid content-start gap-4 p-4 sm:p-5')}>
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-black uppercase text-emerald-700">Hồ sơ của tôi</p>
              <h2 className="mt-1 text-xl font-black text-slate-950">{fullName(profile)}</h2>
            </div>
            <ProfileBadge profile={profile} />
          </div>
          <div className="grid gap-3">
            {[
              ['Số điện thoại', profile?.phone || 'Chưa cập nhật'],
              ['Ngày sinh', profile?.dateOfBirth || 'Chưa cập nhật'],
              ['Giới tính', GENDER_LABELS[profile?.gender] || 'Chưa cập nhật'],
              ['Địa chỉ', profile?.address || 'Chưa cập nhật'],
            ].map(([label, value]) => (
              <div className="flex items-start justify-between gap-4 rounded-lg bg-slate-50 p-3" key={label}>
                <span className="text-sm font-bold text-slate-500">{label}</span>
                <strong className="text-right text-sm font-black text-slate-900">{value}</strong>
              </div>
            ))}
          </div>
        </article>

        <article className={cx(panelClass, 'grid content-start gap-4 p-4 sm:p-5')}>
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-xs font-black uppercase text-emerald-700">Đã xác nhận</p>
              <h2 className="mt-1 text-xl font-black text-slate-950">Người đang hỗ trợ</h2>
            </div>
            <HeartHandshake className="text-emerald-600" size={22} />
          </div>
          <div className="grid gap-3">
            {acceptedCaregivers.length ? (
              acceptedCaregivers.map((caregiver) => <RelationshipCard key={caregiver.relationshipId} profile={caregiver} />)
            ) : (
              <EmptyState>Chưa có người hỗ trợ nào được xác nhận.</EmptyState>
            )}
          </div>
        </article>
      </section>

      <section className={cx(panelClass, 'grid gap-4 p-4 sm:p-5')}>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-xs font-black uppercase text-emerald-700">Lời mời</p>
            <h2 className="mt-1 text-xl font-black text-slate-950">Người hỗ trợ đang chờ xác nhận</h2>
          </div>
          <ShieldCheck className="text-sky-600" size={22} />
        </div>
        <div className="grid gap-3">
          {pendingCaregivers.length ? (
            pendingCaregivers.map((caregiver) => (
              <article
                className="grid gap-4 rounded-lg border border-slate-200 bg-white p-4 shadow-sm shadow-slate-100 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center"
                key={caregiver.relationshipId}
              >
                <div className="flex min-w-0 items-start gap-3">
                  <ProfileBadge profile={caregiver} />
                  <div className="min-w-0">
                    <strong className="block truncate text-sm font-black text-slate-950">{fullName(caregiver)}</strong>
                    <span className="mt-1 block truncate text-sm font-bold text-slate-600">
                      {caregiver.phone || caregiver.caregiverTitle || 'Đang chờ xác nhận'}
                    </span>
                  </div>
                </div>
                <div className="flex flex-wrap gap-2 sm:justify-end">
                  <AppButton size="sm" tone="secondary" onClick={() => handleAccept(caregiver.relationshipId)}>
                    <Check size={14} />
                    Chấp nhận
                  </AppButton>
                  <AppButton size="sm" tone="ghost" onClick={() => handleRefuse(caregiver.relationshipId)}>
                    <X size={14} />
                    Từ chối
                  </AppButton>
                </div>
              </article>
            ))
          ) : (
            <EmptyState>Không có lời mời nào đang chờ.</EmptyState>
          )}
        </div>
      </section>
    </div>
  )
}

export function RelationshipsPage() {
  const activeRole = useAuthStore((state) => state.activeProfile?.role)

  if (activeRole === 'ELDERLY') {
    return <ElderlyRelationshipsPage />
  }

  return <CaregiverRelationshipsPage />
}

export default RelationshipsPage
