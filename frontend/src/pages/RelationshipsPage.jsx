import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  CalendarClock,
  Check,
  ClipboardList,
  Eye,
  HeartHandshake,
  LoaderCircle,
  MapPin,
  MessageCircle,
  NotebookText,
  Pencil,
  Phone,
  Pill,
  Plus,
  ScanSearch,
  Search,
  Send,
  ShieldCheck,
  Trash2,
  UserRound,
  UsersRound,
  X,
} from 'lucide-react'

import { Button } from '../components/ui/Button.jsx'
import { ConfirmDialog } from '../components/ui/Modal.jsx'
import { notify } from '../lib/toast.js'
import { useAuthStore } from '../modules/auth/authStore.js'
import {
  acceptCaregiverInvitation,
  createManagedElderlyProfile,
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
} from '../modules/profile/profileApi.js'
import '../styles/profile-management/profile-management.css'

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
  elderlyTitle: 'Người thân',
  permissionLevel: 'MANAGE_ALL',
}

const PERMISSION_LABELS = {
  VIEW: 'Chỉ xem',
  EDIT_SCHEDULE: 'Sửa lịch uống',
  MANAGE_ALL: 'Toàn quyền quản lý',
}

const PROFILE_ROLE_LABELS = {
  ADMIN: 'Quản trị',
  CAREGIVER: 'Người chăm sóc',
  ELDERLY: 'Người thân',
}

const STATUS_LABELS = {
  ACCEPTED: 'Đã xác nhận',
  PENDING: 'Đang chờ',
  REVOKED: 'Đã thu hồi',
  REFUSED: 'Đã từ chối',
}

const RELATIONSHIP_FILTERS = [
  { value: 'ACCEPTED', label: 'Đã xác nhận' },
  { value: 'PENDING', label: 'Đang chờ' },
  { value: 'REVOKED', label: 'Đã thu hồi' },
]

const GENDER_LABELS = {
  MALE: 'Nam',
  FEMALE: 'Nữ',
  OTHER: 'Khác',
}

function fullName(profile) {
  return [profile?.firstName, profile?.lastName].filter(Boolean).join(' ').trim() || 'Hồ sơ PharmAgent'
}

function initials(profile) {
  const parts = fullName(profile).split(/\s+/).filter(Boolean)
  return ((parts[0]?.[0] ?? 'P') + (parts.at(-1)?.[0] ?? '')).slice(0, 2).toUpperCase()
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
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

function buildProfilePayload(form) {
  return {
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    phone: form.phone.trim(),
    dateOfBirth: form.dateOfBirth || null,
    gender: form.gender,
    address: normalizeOptional(form.address),
    avatarUrl: normalizeOptional(form.avatarUrl),
    role: 'ELDERLY',
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
    status,
  }
}

function relationshipKey(profile) {
  return profile?.profileId ?? profile?.id ?? profile?.relationshipId
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

function ProfileBadge({ profile }) {
  return (
    <div className="profile-mini-avatar">
      {profile?.avatarUrl ? <img src={profile.avatarUrl} alt={`Ảnh đại diện ${fullName(profile)}`} /> : <span>{initials(profile)}</span>}
    </div>
  )
}

function PermissionSelect({ value, onChange }) {
  return (
    <select className="profile-permission-select" value={value ?? 'VIEW'} onChange={(event) => onChange(event.target.value)}>
      <option value="VIEW">{PERMISSION_LABELS.VIEW}</option>
      <option value="EDIT_SCHEDULE">{PERMISSION_LABELS.EDIT_SCHEDULE}</option>
      <option value="MANAGE_ALL">{PERMISSION_LABELS.MANAGE_ALL}</option>
    </select>
  )
}

function EmptyState({ children }) {
  return <div className="profile-management-empty">{children}</div>
}

function ProfileDrawer({ editTarget, form, loading, open, onClose, onSubmit, setField }) {
  if (!open) {
    return null
  }

  const isEdit = Boolean(editTarget)

  return (
    <>
      <div className="profile-drawer-overlay" onClick={onClose} />
      <aside className="profile-drawer" aria-modal="true" role="dialog">
        <div className="profile-drawer-header">
          <div>
            <p className="eyebrow">Người thân</p>
            <h2>{isEdit ? 'Chỉnh sửa hồ sơ' : 'Tạo hồ sơ mới'}</h2>
          </div>
          <button className="icon-button" type="button" aria-label="Đóng" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <form className="profile-management-form" onSubmit={onSubmit}>
          <div className="profile-form-row">
            <label>
              <span>Họ</span>
              <input value={form.firstName} onChange={setField('firstName')} />
            </label>
            <label>
              <span>Tên</span>
              <input value={form.lastName} onChange={setField('lastName')} />
            </label>
          </div>

          <div className="profile-form-row">
            <label>
              <span>Số điện thoại</span>
              <input value={form.phone} onChange={setField('phone')} />
            </label>
            <label>
              <span>Ngày sinh</span>
              <input type="date" value={form.dateOfBirth} onChange={setField('dateOfBirth')} />
            </label>
          </div>

          <div className="profile-form-row">
            <label>
              <span>Giới tính</span>
              <select value={form.gender} onChange={setField('gender')}>
                <option value="MALE">Nam</option>
                <option value="FEMALE">Nữ</option>
                <option value="OTHER">Khác</option>
              </select>
            </label>
            <label>
              <span>Avatar URL</span>
              <input value={form.avatarUrl} onChange={setField('avatarUrl')} />
            </label>
          </div>

          <label>
            <span>Địa chỉ</span>
            <textarea value={form.address} onChange={setField('address')} />
          </label>

          <div className="profile-management-actions">
            <Button type="button" variant="ghost" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? <LoaderCircle className="profile-management-spin" size={16} /> : <Check size={16} />}
              {isEdit ? 'Lưu hồ sơ' : 'Tạo hồ sơ'}
            </Button>
          </div>
        </form>
      </aside>
    </>
  )
}

function StatusPill({ status }) {
  const normalizedStatus = (status || '').toUpperCase()

  return <span className={`relationship-status-pill relationship-status-pill--${normalizedStatus.toLowerCase()}`}>{statusLabel(normalizedStatus)}</span>
}

function RelationshipStatCard({ icon: Icon, label, tone, value }) {
  return (
    <article className={`relationship-stat-card relationship-stat-card--${tone}`}>
      <span>
        <Icon size={18} />
      </span>
      <div>
        <strong>{value}</strong>
        <p>{label}</p>
      </div>
    </article>
  )
}

function LocalRelativeCard({ profile, onDelete, onEdit, onView }) {
  return (
    <article className="profile-person-card relationship-local-card">
      <div className="profile-person-main">
        <ProfileBadge profile={profile} />
        <div>
          <strong>{fullName(profile)}</strong>
          <span>{profile.phone || 'Chưa cập nhật số điện thoại'}</span>
          <span>{profile.address || 'Chưa cập nhật địa chỉ'}</span>
        </div>
      </div>
      <div className="profile-person-actions">
        <Button size="sm" variant="secondary" onClick={() => onView(profile)}>
          <Eye size={15} />
          Chi tiết
        </Button>
        <Button size="sm" variant="ghost" onClick={() => onEdit(profile)}>
          <Pencil size={15} />
          Sửa
        </Button>
        <Button size="sm" variant="danger" onClick={() => onDelete(profile)}>
          <Trash2 size={15} />
          Xóa
        </Button>
      </div>
    </article>
  )
}

function RelationshipCard({ profile, onPermissionChange }) {
  const [permission, setPermission] = useState(profile.permissionLevel ?? 'VIEW')
  const canUpdate = typeof onPermissionChange === 'function'

  return (
    <article className="profile-person-card">
      <div className="profile-person-main">
        <ProfileBadge profile={profile} />
        <div>
          <strong>{fullName(profile)}</strong>
          <span>{profile.phone || profile.address || 'Chưa cập nhật thông tin liên hệ'}</span>
        </div>
      </div>
      <div className="profile-person-meta">
        {profile.elderlyTitle || profile.caregiverTitle ? (
          <span>{profile.elderlyTitle || profile.caregiverTitle}</span>
        ) : null}
        {canUpdate ? (
          <div className="profile-permission-row">
            <PermissionSelect value={permission} onChange={setPermission} />
            <Button size="sm" variant="secondary" onClick={() => onPermissionChange(profile, permission)}>
              Cập nhật
            </Button>
          </div>
        ) : (
          <span>{PERMISSION_LABELS[profile.permissionLevel] ?? 'Chưa cấp quyền'}</span>
        )}
      </div>
    </article>
  )
}

function RelationshipQuickActions({ profile, onNavigate }) {
  const hasProfile = Boolean(profile)
  const phoneHref = profile?.phone ? `tel:${profile.phone}` : undefined

  return (
    <div className="relationship-quick-actions">
      <Button disabled={!hasProfile} variant="secondary" onClick={() => onNavigate('/medications', 'add')}>
        <Pill size={16} />
        Thêm thuốc
      </Button>
      <Button disabled={!hasProfile} variant="ghost" onClick={() => onNavigate('/dashboard', 'schedule')}>
        <CalendarClock size={16} />
        Sửa lịch
      </Button>
      <Button disabled={!hasProfile} variant="ghost" onClick={() => onNavigate('/chat')}>
        <MessageCircle size={16} />
        Chat
      </Button>
      <a
        aria-disabled={!phoneHref}
        className={`btn btn--ghost btn--md relationship-action-link ${!phoneHref ? 'is-disabled' : ''}`}
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
      <Button disabled={!hasProfile} variant="ghost" onClick={() => onNavigate('/scan')}>
        <ScanSearch size={16} />
        Scan
      </Button>
    </div>
  )
}

function RelationshipTimeline({ profile }) {
  if (!profile) {
    return <EmptyState>Chọn một người thân để xem sự kiện và ghi chú gần đây.</EmptyState>
  }

  const items = [
    {
      icon: ShieldCheck,
      tone: 'green',
      title: statusLabel(profile.status),
      body: profile.status === 'PENDING' ? 'Lời mời đã gửi và đang chờ phản hồi.' : 'Kết nối đã sẵn sàng cho các nghiệp vụ chăm sóc.',
      meta: 'Trạng thái',
    },
    {
      icon: ClipboardList,
      tone: 'blue',
      title: PERMISSION_LABELS[profile.permissionLevel] ?? 'Chưa cấp quyền',
      body: 'Quyền này quyết định phạm vi thao tác với thuốc, lịch uống và thông tin liên quan.',
      meta: 'Phân quyền',
    },
    {
      icon: NotebookText,
      tone: 'amber',
      title: 'Ghi chú gần đây',
      body: 'Chưa có ghi chú mới từ dữ liệu backend. Khu vực này đã sẵn sàng để hiển thị notes khi API bổ sung.',
      meta: 'Notes',
    },
  ]

  return (
    <div className="relationship-timeline">
      {items.map((item) => {
        const Icon = item.icon

        return (
          <article className={`relationship-timeline-item relationship-timeline-item--${item.tone}`} key={item.title}>
            <span>
              <Icon size={16} />
            </span>
            <div>
              <small>{item.meta}</small>
              <strong>{item.title}</strong>
              <p>{item.body}</p>
            </div>
          </article>
        )
      })}
    </div>
  )
}

function RelationshipOverview({ profile, onNavigate }) {
  return (
    <article className="relationship-overview-panel">
      <div className="relationship-overview-header">
        <div className="relationship-overview-person">
          <ProfileBadge profile={profile} />
          <div>
            <p className="eyebrow">Tổng quan người thân</p>
            <h2>{profile ? fullName(profile) : 'Chưa chọn hồ sơ'}</h2>
            <span>{profile?.elderlyTitle || 'Chưa đặt cách gọi'}</span>
          </div>
        </div>
        {profile ? <StatusPill status={profile.status} /> : null}
      </div>

      <div className="relationship-overview-facts">
        <div>
          <Phone size={15} />
          <span>{profile?.phone || 'Chưa cập nhật số điện thoại'}</span>
        </div>
        <div>
          <MapPin size={15} />
          <span>{profile?.address || 'Chưa cập nhật địa chỉ'}</span>
        </div>
        <div>
          <ShieldCheck size={15} />
          <span>{profile ? PERMISSION_LABELS[profile.permissionLevel] ?? 'Chưa cấp quyền' : 'Chưa có quyền'}</span>
        </div>
      </div>

      <RelationshipQuickActions profile={profile} onNavigate={onNavigate} />

      <div className="relationship-section-heading">
        <div>
          <p className="eyebrow">Theo dõi gần đây</p>
          <h3>Sự kiện và ghi chú</h3>
        </div>
      </div>
      <RelationshipTimeline profile={profile} />
    </article>
  )
}

function RelationshipTable({ onChat, onEditPermission, onView, relationships, selectedKey }) {
  if (!relationships.length) {
    return <EmptyState>Không có người thân trong trạng thái này.</EmptyState>
  }

  return (
    <div className="relationship-table-wrap">
      <table className="relationship-table">
        <thead>
          <tr>
            <th>Người thân</th>
            <th>Số điện thoại</th>
            <th>Địa chỉ</th>
            <th>Cách gọi</th>
            <th>Quyền</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {relationships.map((profile) => {
            const key = relationshipKey(profile)
            const isSelected = key === selectedKey
            const phoneHref = profile.phone ? `tel:${profile.phone}` : undefined

            return (
              <tr className={isSelected ? 'is-selected' : ''} key={profile.relationshipId ?? key}>
                <td>
                  <button className="relationship-person-cell" type="button" onClick={() => onView(profile)}>
                    <ProfileBadge profile={profile} />
                    <span>
                      <strong>{fullName(profile)}</strong>
                      <small>{profile.elderlyTitle || 'Chưa đặt cách gọi'}</small>
                    </span>
                  </button>
                </td>
                <td>{profile.phone || 'Chưa cập nhật'}</td>
                <td>{profile.address || 'Chưa cập nhật'}</td>
                <td>{profile.elderlyTitle || 'Chưa cập nhật'}</td>
                <td>
                  <span className="relationship-permission-chip">{PERMISSION_LABELS[profile.permissionLevel] ?? 'Chưa cấp quyền'}</span>
                </td>
                <td>
                  <StatusPill status={profile.status} />
                </td>
                <td>
                  <div className="relationship-row-actions">
                    <Button size="sm" variant="secondary" onClick={() => onView(profile)}>
                      <Eye size={14} />
                      Chi tiết
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => onChat(profile)}>
                      <MessageCircle size={14} />
                      Chat
                    </Button>
                    <a
                      aria-disabled={!phoneHref}
                      className={`btn btn--ghost btn--sm relationship-action-link ${!phoneHref ? 'is-disabled' : ''}`}
                      href={phoneHref}
                      onClick={(event) => {
                        if (!phoneHref) {
                          event.preventDefault()
                        }
                      }}
                    >
                      <Phone size={14} />
                      Gọi
                    </a>
                    <Button size="sm" variant="ghost" onClick={() => onEditPermission(profile)}>
                      <Pencil size={14} />
                      Quyền
                    </Button>
                  </div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
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
  if (!open) {
    return null
  }

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
    <>
      <div className="profile-drawer-overlay" onClick={onClose} />
      <aside className="profile-drawer relationship-invite-drawer" aria-modal="true" role="dialog">
        <div className="profile-drawer-header">
          <div>
            <p className="eyebrow">Lời mời mới</p>
            <h2>Mời người thân mới</h2>
          </div>
          <button className="icon-button" type="button" aria-label="Đóng" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <form className="relationship-invite-search" onSubmit={onSearch}>
          <label>
            <span>Tìm theo tên, số điện thoại hoặc email</span>
            <div className="profile-search-input-wrap">
              <Search size={16} />
              <input
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                placeholder="Ví dụ: Nguyễn An, 090..., an@email.com"
              />
            </div>
          </label>
          <Button type="submit" disabled={searching}>
            {searching ? <LoaderCircle className="profile-management-spin" size={16} /> : <Search size={16} />}
            Tìm hồ sơ
          </Button>
        </form>

        <div className="relationship-invite-results">
          {searchResults.length ? (
            searchResults.map((profile) => {
              const state = invitationState(profile)
              const disabled = state !== 'available'
              const isSelected = relationshipKey(inviteTarget) === (profile.id ?? profile.profileId)

              return (
                <article className={`relationship-search-result ${isSelected ? 'is-selected' : ''}`} key={profile.id ?? profile.profileId}>
                  <div className="profile-person-main">
                    <ProfileBadge profile={profile} />
                    <div>
                      <strong>{fullName(profile)}</strong>
                      <span>{profile.phone || 'Chưa cập nhật số điện thoại'}</span>
                    </div>
                  </div>
                  <div className="relationship-search-result-side">
                    <span className="relationship-role-chip">{PROFILE_ROLE_LABELS[profile.role] ?? 'Người thân'}</span>
                    <Button disabled={disabled} size="sm" variant={isSelected ? 'secondary' : 'ghost'} onClick={() => setInviteTarget(profile)}>
                      {state === 'accepted' ? 'Đã kết nối' : state === 'pending' ? 'Đang chờ' : isSelected ? 'Đã chọn' : 'Chọn'}
                    </Button>
                  </div>
                </article>
              )
            })
          ) : (
            <EmptyState>Nhập từ khóa để tìm hồ sơ ngoài tài khoản hiện tại.</EmptyState>
          )}
        </div>

        <form className="profile-management-form relationship-invite-form" onSubmit={onInvite}>
          <div className="relationship-selected-invite">
            {inviteTarget ? (
              <>
                <ProfileBadge profile={inviteTarget} />
                <div>
                  <span>Hồ sơ sẽ nhận lời mời</span>
                  <strong>{fullName(inviteTarget)}</strong>
                  <small>{inviteTarget.phone || inviteTarget.email || 'Chưa cập nhật thông tin liên hệ'}</small>
                </div>
              </>
            ) : (
              <span>Chọn một hồ sơ trong danh sách kết quả trước khi gửi lời mời.</span>
            )}
          </div>

          <div className="profile-form-row">
            <label>
              <span>Cách người thân gọi bạn</span>
              <input value={inviteForm.caregiverTitle} onChange={setInviteField('caregiverTitle')} />
            </label>
            <label>
              <span>Cách bạn gọi người thân</span>
              <input value={inviteForm.elderlyTitle} onChange={setInviteField('elderlyTitle')} />
            </label>
          </div>

          <label>
            <span>Mức quyền đề nghị</span>
            <PermissionSelect
              value={inviteForm.permissionLevel}
              onChange={(value) => setInviteForm((current) => ({ ...current, permissionLevel: value }))}
            />
          </label>

          <div className="profile-management-actions">
            <Button type="button" variant="ghost" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" disabled={!inviteTarget || targetState !== 'available' || Boolean(invitingId)}>
              {invitingId ? <LoaderCircle className="profile-management-spin" size={16} /> : <Send size={16} />}
              {targetState === 'pending' ? 'Đã gửi lời mời' : 'Gửi lời mời'}
            </Button>
          </div>
        </form>
      </aside>
    </>
  )
}

function PermissionDrawer({ loading, onClose, onSubmit, permission, setPermission, target }) {
  if (!target) {
    return null
  }

  return (
    <>
      <div className="profile-drawer-overlay" onClick={onClose} />
      <aside className="profile-drawer relationship-permission-drawer" aria-modal="true" role="dialog">
        <div className="profile-drawer-header">
          <div>
            <p className="eyebrow">Phân quyền</p>
            <h2>Sửa quyền chăm sóc</h2>
          </div>
          <button className="icon-button" type="button" aria-label="Đóng" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <div className="relationship-selected-invite">
          <ProfileBadge profile={target} />
          <div>
            <span>Người thân</span>
            <strong>{fullName(target)}</strong>
            <small>{target.phone || target.address || 'Chưa cập nhật thông tin liên hệ'}</small>
          </div>
        </div>

        <form className="profile-management-form" onSubmit={onSubmit}>
          <label>
            <span>Mức quyền mới</span>
            <PermissionSelect value={permission} onChange={setPermission} />
          </label>
          <div className="profile-management-actions">
            <Button type="button" variant="ghost" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? <LoaderCircle className="profile-management-spin" size={16} /> : <Check size={16} />}
              Gửi cập nhật
            </Button>
          </div>
        </form>
      </aside>
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
  const [permissionTarget, setPermissionTarget] = useState(null)
  const [permissionForm, setPermissionForm] = useState('VIEW')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [searching, setSearching] = useState(false)
  const [updatingPermission, setUpdatingPermission] = useState(false)

  const loadCaregiverData = useCallback(async () => {
    setLoading(true)
    try {
      const [profilesPage, accepted, pending] = await Promise.all([
        getProfiles({ page: 0, size: 100 }),
        getCaregiverRelationships(),
        getPendingCaregiverRelationships(),
      ])
      const accountProfiles = asPageContent(profilesPage)
      const elderlyProfiles = accountProfiles.filter((profile) => profile.role === 'ELDERLY')
      setLocalProfiles(elderlyProfiles)
      setAcceptedRelationships(Array.isArray(accepted) ? accepted : [])
      setPendingRelationships(Array.isArray(pending) ? pending : [])
      replaceProfiles([useAuthStore.getState().activeProfile, ...accountProfiles].filter(Boolean))
    } catch (err) {
      notify.apiError(err, 'Không thể tải danh sách người thân')
    } finally {
      setLoading(false)
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

  function openCreateDrawer() {
    setEditTarget(null)
    setForm(EMPTY_PROFILE_FORM)
    setDrawerOpen(true)
  }

  function openInviteDrawer() {
    setInviteTarget(null)
    setSearchQuery('')
    setSearchResults([])
    setInviteForm(EMPTY_INVITE_FORM)
    setInviteDrawerOpen(true)
  }

  async function openEditDrawer(profile) {
    setEditTarget(profile)
    setDrawerOpen(true)
    setForm(profileToForm(profile))
    try {
      const detail = await getManagedElderlyProfile(profile.id)
      setEditTarget(detail)
      setForm(profileToForm(detail))
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
    if (!validateProfileForm()) {
      return
    }

    setSaving(true)
    try {
      if (editTarget?.id) {
        await updateManagedElderlyProfile(editTarget.id, buildProfilePayload(form))
        notify.success('Đã cập nhật hồ sơ người thân')
      } else {
        await createManagedElderlyProfile(buildProfilePayload(form))
        notify.success('Đã tạo hồ sơ người thân')
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
      await deleteManagedElderlyProfile(deleteTarget.id)
      notify.success('Đã xóa hồ sơ người thân')
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
        elderlyTitle: normalizeOptional(inviteForm.elderlyTitle),
        permissionLevel: inviteForm.permissionLevel,
      })
      setInvitedProfileIds((current) => new Set(current).add(targetId))
      notify.success('Đã gửi lời mời')
      await loadCaregiverData()
    } catch (err) {
      notify.apiError(err, 'Không thể gửi lời mời')
    } finally {
      setInvitingId(null)
    }
  }

  async function handleSavePermission(event) {
    event.preventDefault()
    if (!permissionTarget) {
      return
    }

    setUpdatingPermission(true)
    try {
      await updateCaregiverRelationship(relationshipKey(permissionTarget), permissionForm)
      notify.success('Đã gửi yêu cầu cập nhật quyền')
      setPermissionTarget(null)
      await loadCaregiverData()
    } catch (err) {
      notify.apiError(err, 'Không thể cập nhật quyền')
    } finally {
      setUpdatingPermission(false)
    }
  }

  function openPermissionDrawer(profile) {
    setPermissionTarget(profile)
    setPermissionForm(profile.permissionLevel ?? 'VIEW')
  }

  function handleNavigate(path, action, profile) {
    const targetProfile = profile ?? selectedProfile
    navigate(`${path}${profileQuery(targetProfile, action)}`)
  }

  function handleViewRelationship(profile) {
    setSelectedRelationshipId(relationshipKey(profile))
  }

  function handleViewLocalProfile(profile) {
    setSelectedRelationshipId(profile.id)
  }

  function handleChat(profile) {
    navigate(`/chat${profileQuery(profile)}`)
  }

  const allRelationships = useMemo(
    () => [
      ...acceptedRelationships.map((profile) => normalizeRelationship(profile, 'ACCEPTED')),
      ...pendingRelationships.map((profile) => normalizeRelationship(profile, 'PENDING')),
    ],
    [acceptedRelationships, pendingRelationships],
  )

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
    const availableIds = [
      ...allRelationships.map((profile) => relationshipKey(profile)),
      ...localProfiles.map((profile) => profile.id),
    ].filter(Boolean)

    if (!availableIds.length) {
      setSelectedRelationshipId(null)
      return
    }

    if (!selectedRelationshipId || !availableIds.includes(selectedRelationshipId)) {
      setSelectedRelationshipId(availableIds[0])
    }
  }, [allRelationships, localProfiles, selectedRelationshipId])

  const selectedRelationship = allRelationships.find((profile) => relationshipKey(profile) === selectedRelationshipId)
  const selectedLocalProfile = localProfiles.find((profile) => profile.id === selectedRelationshipId)
  const selectedProfile = selectedRelationship ?? (selectedLocalProfile ? normalizeRelationship({
    ...selectedLocalProfile,
    elderlyTitle: 'Người thân trong tài khoản',
    permissionLevel: 'MANAGE_ALL',
    profileId: selectedLocalProfile.id,
    status: 'ACCEPTED',
  }) : null)

  if (loading) {
    return (
      <div className="profile-management-loading">
        <LoaderCircle className="profile-management-spin" size={24} />
        Đang tải người thân...
      </div>
    )
  }

  return (
    <div className="profile-management-page relationships-page">
      <section className="profile-management-hero profile-management-hero--split relationship-hero">
        <div>
          <p className="eyebrow">Không gian chăm sóc</p>
          <h1>Người thân của tôi</h1>
          <p>Xem tổng quan từng người thân, theo dõi trạng thái kết nối và đi nhanh đến thuốc, lịch uống, chat, gọi hoặc scan thuốc.</p>
        </div>
        <div className="relationship-hero-actions">
          <Button variant="secondary" onClick={openCreateDrawer}>
            <Plus size={16} />
            Tạo hồ sơ trong tài khoản
          </Button>
          <Button onClick={openInviteDrawer}>
            <Send size={16} />
            Mời người thân mới
          </Button>
        </div>
      </section>

      <section className="relationship-stats-grid">
        <RelationshipStatCard icon={HeartHandshake} label="Đã xác nhận" tone="green" value={relationshipCounts.ACCEPTED ?? 0} />
        <RelationshipStatCard icon={ShieldCheck} label="Đang chờ phản hồi" tone="blue" value={relationshipCounts.PENDING ?? 0} />
        <RelationshipStatCard icon={UsersRound} label="Hồ sơ trong tài khoản" tone="amber" value={localProfiles.length} />
      </section>

      <section className="relationship-workspace">
        <RelationshipOverview profile={selectedProfile} onNavigate={handleNavigate} />

        <article className="relationship-panel relationship-directory-panel">
          <div className="profile-management-panel-header">
            <div>
              <p className="eyebrow">Danh sách kết nối</p>
              <h2>Người thân theo trạng thái</h2>
            </div>
            <HeartHandshake size={20} />
          </div>

          <div className="relationship-filter-tabs" role="tablist" aria-label="Lọc trạng thái người thân">
            {RELATIONSHIP_FILTERS.map((filter) => (
              <button
                aria-selected={relationshipFilter === filter.value}
                className={relationshipFilter === filter.value ? 'is-active' : ''}
                key={filter.value}
                role="tab"
                type="button"
                onClick={() => setRelationshipFilter(filter.value)}
              >
                <span>{filter.label}</span>
                <strong>{relationshipCounts[filter.value] ?? 0}</strong>
              </button>
            ))}
          </div>

          <RelationshipTable
            relationships={filteredRelationships}
            selectedKey={relationshipKey(selectedProfile)}
            onChat={handleChat}
            onEditPermission={openPermissionDrawer}
            onView={handleViewRelationship}
          />
        </article>
      </section>

      <section className="profile-management-grid relationship-secondary-grid">
        <article className="profile-management-panel profile-management-panel--wide">
          <div className="profile-management-panel-header">
            <div>
              <p className="eyebrow">Trong tài khoản</p>
              <h2>Hồ sơ người thân đã tạo</h2>
            </div>
            <UsersRound size={20} />
          </div>

          <div className="profile-list-stack">
            {localProfiles.length ? (
              localProfiles.map((profile) => (
                <LocalRelativeCard
                  key={profile.id}
                  profile={profile}
                  onDelete={setDeleteTarget}
                  onEdit={openEditDrawer}
                  onView={handleViewLocalProfile}
                />
              ))
            ) : (
              <EmptyState>Chưa có hồ sơ người thân nào trong tài khoản này.</EmptyState>
            )}
          </div>
        </article>
      </section>

      <ProfileDrawer
        editTarget={editTarget}
        form={form}
        loading={saving}
        open={drawerOpen}
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

      <PermissionDrawer
        loading={updatingPermission}
        permission={permissionForm}
        setPermission={setPermissionForm}
        target={permissionTarget}
        onClose={() => setPermissionTarget(null)}
        onSubmit={handleSavePermission}
      />

      <ConfirmDialog
        confirmLabel="Xóa hồ sơ"
        description={`Bạn có chắc muốn xóa hồ sơ "${fullName(deleteTarget)}"?`}
        open={Boolean(deleteTarget)}
        title="Xóa hồ sơ người thân"
        onConfirm={handleDeleteProfile}
        onOpenChange={(open) => {
          if (!open) {
            setDeleteTarget(null)
          }
        }}
      />
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
      notify.apiError(err, 'Không thể tải thông tin người chăm sóc')
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
      notify.success('Đã chấp nhận lời mời chăm sóc')
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
    return (
      <div className="profile-management-loading">
        <LoaderCircle className="profile-management-spin" size={24} />
        Đang tải người chăm sóc...
      </div>
    )
  }

  return (
    <div className="profile-management-page">
      <section className="profile-management-hero profile-management-hero--split">
        <div>
          <p className="eyebrow">Hồ sơ cá nhân</p>
          <h1>Người chăm sóc của tôi</h1>
          <p>Hồ sơ của bạn chỉ xem được thông tin cá nhân và danh sách người đã được kết nối.</p>
        </div>
        <div className="profile-readonly-chip">
          <UserRound size={16} />
          Chỉ xem hồ sơ
        </div>
      </section>

      <section className="profile-management-grid">
        <article className="profile-management-panel">
          <div className="profile-management-panel-header">
            <div>
              <p className="eyebrow">Hồ sơ của tôi</p>
              <h2>{fullName(profile)}</h2>
            </div>
            <ProfileBadge profile={profile} />
          </div>
          <div className="profile-info-list">
            <div className="profile-info-row">
              <span>Số điện thoại</span>
              <strong>{profile?.phone || 'Chưa cập nhật'}</strong>
            </div>
            <div className="profile-info-row">
              <span>Ngày sinh</span>
              <strong>{profile?.dateOfBirth || 'Chưa cập nhật'}</strong>
            </div>
            <div className="profile-info-row">
              <span>Giới tính</span>
              <strong>{GENDER_LABELS[profile?.gender] || 'Chưa cập nhật'}</strong>
            </div>
            <div className="profile-info-row">
              <span>Địa chỉ</span>
              <strong>{profile?.address || 'Chưa cập nhật'}</strong>
            </div>
          </div>
        </article>

        <article className="profile-management-panel">
          <div className="profile-management-panel-header">
            <div>
              <p className="eyebrow">Đã xác nhận</p>
              <h2>Người đang chăm sóc</h2>
            </div>
            <HeartHandshake size={20} />
          </div>
          <div className="profile-list-stack">
            {acceptedCaregivers.length ? (
              acceptedCaregivers.map((caregiver) => (
                <RelationshipCard key={caregiver.relationshipId} profile={caregiver} />
              ))
            ) : (
              <EmptyState>Chưa có người chăm sóc nào được xác nhận.</EmptyState>
            )}
          </div>
        </article>

        <article className="profile-management-panel profile-management-panel--wide">
          <div className="profile-management-panel-header">
            <div>
              <p className="eyebrow">Lời mời</p>
              <h2>Người chăm sóc đang chờ xác nhận</h2>
            </div>
            <ShieldCheck size={20} />
          </div>
          <div className="profile-list-stack">
            {pendingCaregivers.length ? (
              pendingCaregivers.map((caregiver) => (
                <article className="profile-person-card" key={caregiver.relationshipId}>
                  <div className="profile-person-main">
                    <ProfileBadge profile={caregiver} />
                    <div>
                      <strong>{fullName(caregiver)}</strong>
                      <span>{caregiver.phone || caregiver.caregiverTitle || 'Đang chờ xác nhận'}</span>
                    </div>
                  </div>
                  <div className="profile-person-actions">
                    <Button size="sm" variant="secondary" onClick={() => handleAccept(caregiver.relationshipId)}>
                      <Check size={15} />
                      Chấp nhận
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => handleRefuse(caregiver.relationshipId)}>
                      <X size={15} />
                      Từ chối
                    </Button>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState>Không có lời mời nào đang chờ.</EmptyState>
            )}
          </div>
        </article>
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
