import { useEffect, useMemo, useState } from 'react'
import {
  ContactRound,
  Eye,
  KeyRound,
  LoaderCircle,
  Mail,
  Pencil,
  Plus,
  Save,
  ShieldCheck,
  Trash2,
  X,
} from 'lucide-react'

import { Button } from '../components/ui/Button.jsx'
import { ConfirmDialog } from '../components/ui/Modal.jsx'
import { notify } from '../lib/toast.js'
import { useAuthStore } from '../modules/auth/authStore.js'
import {
  addMyContact,
  changePassword,
  deleteMyContact,
  getMyContacts,
  getMyProfile,
  updateMyContact,
  updateMyProfile,
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

const EMPTY_PASSWORD_FORM = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
}

const EMPTY_CONTACT_FORM = {
  name: '',
  phone: '',
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

function profileName(profile) {
  return [profile?.firstName, profile?.lastName].filter(Boolean).join(' ').trim() || 'Hồ sơ PharmAgent'
}

function profileInitials(profile) {
  const parts = profileName(profile).split(/\s+/).filter(Boolean)
  return ((parts[0]?.[0] ?? 'P') + (parts.at(-1)?.[0] ?? '')).slice(0, 2).toUpperCase()
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
  const trimmed = value.trim()
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

function InfoRow({ label, value }) {
  return (
    <div className="profile-info-row">
      <span>{label}</span>
      <strong>{value || 'Chưa cập nhật'}</strong>
    </div>
  )
}

function ProfileAvatar({ profile }) {
  return (
    <div className="profile-management-avatar">
      {profile?.avatarUrl ? <img src={profile.avatarUrl} alt="" /> : <span>{profileInitials(profile)}</span>}
    </div>
  )
}

export function ProfileSettingsPage() {
  const activeProfile = useAuthStore((state) => state.activeProfile)
  const mergeActiveProfile = useAuthStore((state) => state.mergeActiveProfile)
  const [profile, setProfile] = useState(null)
  const [profileForm, setProfileForm] = useState(EMPTY_PROFILE_FORM)
  const [passwordForm, setPasswordForm] = useState(EMPTY_PASSWORD_FORM)
  const [contacts, setContacts] = useState([])
  const [contactForm, setContactForm] = useState(EMPTY_CONTACT_FORM)
  const [editingContactId, setEditingContactId] = useState('')
  const [deleteContactTarget, setDeleteContactTarget] = useState(null)
  const [loading, setLoading] = useState(true)
  const [savingProfile, setSavingProfile] = useState(false)
  const [savingPassword, setSavingPassword] = useState(false)
  const [savingContact, setSavingContact] = useState(false)

  const isReadOnly = activeProfile?.role === 'ELDERLY'

  const roleLabel = useMemo(
    () => ROLE_LABELS[profile?.role?.toUpperCase?.()] ?? 'Hồ sơ',
    [profile?.role],
  )

  async function loadSettings() {
    setLoading(true)
    try {
      const [profileData, contactData] = await Promise.all([getMyProfile(), getMyContacts()])
      setProfile(profileData)
      setProfileForm(toProfileForm(profileData))
      setContacts(Array.isArray(contactData) ? contactData : [])
    } catch (err) {
      notify.apiError(err, 'Không thể tải hồ sơ cá nhân')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadSettings()
  }, [])

  const setProfileField = (field) => (event) => {
    setProfileForm((current) => ({ ...current, [field]: event.target.value }))
  }

  const setPasswordField = (field) => (event) => {
    setPasswordForm((current) => ({ ...current, [field]: event.target.value }))
  }

  const setContactField = (field) => (event) => {
    setContactForm((current) => ({ ...current, [field]: event.target.value }))
  }

  async function handleSaveProfile(event) {
    event.preventDefault()

    if (isReadOnly) {
      return
    }

    if (!profileForm.firstName.trim() || !profileForm.lastName.trim() || !profileForm.phone.trim()) {
      notify.warning('Vui lòng nhập đầy đủ họ, tên và số điện thoại')
      return
    }

    setSavingProfile(true)
    try {
      const updated = await updateMyProfile(buildProfilePayload(profileForm))
      setProfile(updated)
      setProfileForm(toProfileForm(updated))
      mergeActiveProfile(updated)
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

  function startEditContact(contact) {
    setEditingContactId(contact.id)
    setContactForm({
      name: contact.name ?? '',
      phone: contact.phone ?? '',
    })
  }

  function resetContactForm() {
    setEditingContactId('')
    setContactForm(EMPTY_CONTACT_FORM)
  }

  async function handleSaveContact(event) {
    event.preventDefault()

    if (isReadOnly) {
      return
    }

    if (!contactForm.name.trim() || !contactForm.phone.trim()) {
      notify.warning('Vui lòng nhập tên và số điện thoại liên hệ')
      return
    }

    setSavingContact(true)
    try {
      const payload = {
        name: contactForm.name.trim(),
        phone: contactForm.phone.trim(),
      }
      const updatedProfile = editingContactId
        ? await updateMyContact(editingContactId, payload)
        : await addMyContact(payload)

      setProfile(updatedProfile)
      setContacts(updatedProfile.userContacts ?? [])
      resetContactForm()
      notify.success(editingContactId ? 'Đã cập nhật liên hệ' : 'Đã thêm liên hệ')
    } catch (err) {
      notify.apiError(err, 'Không thể lưu liên hệ')
    } finally {
      setSavingContact(false)
    }
  }

  async function handleDeleteContact() {
    if (!deleteContactTarget) {
      return
    }

    try {
      const updatedProfile = await deleteMyContact(deleteContactTarget.id)
      setProfile(updatedProfile)
      setContacts(updatedProfile.userContacts ?? [])
      if (editingContactId === deleteContactTarget.id) {
        resetContactForm()
      }
      notify.success('Đã xóa liên hệ')
    } catch (err) {
      notify.apiError(err, 'Không thể xóa liên hệ')
    } finally {
      setDeleteContactTarget(null)
    }
  }

  if (loading) {
    return (
      <div className="profile-management-loading">
        <LoaderCircle className="profile-management-spin" size={24} />
        Đang tải hồ sơ...
      </div>
    )
  }

  return (
    <div className="profile-management-page">
      <section className="profile-management-hero">
        <div className="profile-management-identity">
          <ProfileAvatar profile={profile} />
          <div>
            <p className="eyebrow">Hồ sơ cá nhân</p>
            <h1>{profileName(profile)}</h1>
            <div className="profile-management-meta">
              <span>
                <ShieldCheck size={15} />
                {roleLabel}
              </span>
              {isReadOnly ? (
                <span>
                  <Eye size={15} />
                  Chỉ xem
                </span>
              ) : null}
            </div>
          </div>
        </div>
      </section>

      <section className="profile-management-grid">
        <article className="profile-management-panel">
          <div className="profile-management-panel-header">
            <div>
              <p className="eyebrow">Thông tin hồ sơ</p>
              <h2>{isReadOnly ? 'Thông tin của tôi' : 'Chỉnh sửa hồ sơ'}</h2>
            </div>
            {isReadOnly ? <Eye size={20} /> : <Pencil size={20} />}
          </div>

          {isReadOnly ? (
            <div className="profile-info-list">
              <InfoRow label="Họ và tên" value={profileName(profile)} />
              <InfoRow label="Số điện thoại" value={profile?.phone} />
              <InfoRow label="Ngày sinh" value={profile?.dateOfBirth} />
              <InfoRow label="Giới tính" value={GENDER_LABELS[profile?.gender]} />
              <InfoRow label="Địa chỉ" value={profile?.address} />
            </div>
          ) : (
            <form className="profile-management-form" onSubmit={handleSaveProfile}>
              <div className="profile-form-row">
                <label>
                  <span>Họ</span>
                  <input value={profileForm.firstName} onChange={setProfileField('firstName')} />
                </label>
                <label>
                  <span>Tên</span>
                  <input value={profileForm.lastName} onChange={setProfileField('lastName')} />
                </label>
              </div>

              <div className="profile-form-row">
                <label>
                  <span>Số điện thoại</span>
                  <input value={profileForm.phone} onChange={setProfileField('phone')} />
                </label>
                <label>
                  <span>Ngày sinh</span>
                  <input type="date" value={profileForm.dateOfBirth} onChange={setProfileField('dateOfBirth')} />
                </label>
              </div>

              <div className="profile-form-row">
                <label>
                  <span>Giới tính</span>
                  <select value={profileForm.gender} onChange={setProfileField('gender')}>
                    <option value="MALE">Nam</option>
                    <option value="FEMALE">Nữ</option>
                    <option value="OTHER">Khác</option>
                  </select>
                </label>
                <label>
                  <span>Avatar URL</span>
                  <input value={profileForm.avatarUrl} onChange={setProfileField('avatarUrl')} />
                </label>
              </div>

              <label>
                <span>Địa chỉ</span>
                <textarea value={profileForm.address} onChange={setProfileField('address')} />
              </label>

              <div className="profile-management-actions">
                <Button type="submit" disabled={savingProfile}>
                  {savingProfile ? <LoaderCircle className="profile-management-spin" size={16} /> : <Save size={16} />}
                  Lưu hồ sơ
                </Button>
              </div>
            </form>
          )}
        </article>

        <article className="profile-management-panel">
          <div className="profile-management-panel-header">
            <div>
              <p className="eyebrow">Tài khoản</p>
              <h2>Cài đặt bảo mật</h2>
            </div>
            <KeyRound size={20} />
          </div>

          {isReadOnly ? (
            <div className="profile-management-note">
              Hồ sơ elderly chỉ có quyền xem thông tin cá nhân trong phiên đang chọn.
            </div>
          ) : (
            <form className="profile-management-form" onSubmit={handleChangePassword}>
              <label>
                <span>Mật khẩu hiện tại</span>
                <input
                  type="password"
                  value={passwordForm.currentPassword}
                  onChange={setPasswordField('currentPassword')}
                />
              </label>
              <label>
                <span>Mật khẩu mới</span>
                <input
                  type="password"
                  value={passwordForm.newPassword}
                  onChange={setPasswordField('newPassword')}
                />
              </label>
              <label>
                <span>Xác nhận mật khẩu mới</span>
                <input
                  type="password"
                  value={passwordForm.confirmPassword}
                  onChange={setPasswordField('confirmPassword')}
                />
              </label>
              <div className="profile-management-actions">
                <Button type="submit" disabled={savingPassword}>
                  {savingPassword ? <LoaderCircle className="profile-management-spin" size={16} /> : <KeyRound size={16} />}
                  Đổi mật khẩu
                </Button>
              </div>
            </form>
          )}
        </article>

        <article className="profile-management-panel profile-management-panel--wide">
          <div className="profile-management-panel-header">
            <div>
              <p className="eyebrow">Liên hệ</p>
              <h2>Danh bạ khẩn cấp</h2>
            </div>
            <ContactRound size={20} />
          </div>

          <div className="profile-contact-layout">
            {!isReadOnly ? (
              <form className="profile-management-form profile-contact-form" onSubmit={handleSaveContact}>
                <label>
                  <span>Tên liên hệ</span>
                  <input value={contactForm.name} onChange={setContactField('name')} />
                </label>
                <label>
                  <span>Số điện thoại</span>
                  <input value={contactForm.phone} onChange={setContactField('phone')} />
                </label>
                <div className="profile-management-actions">
                  {editingContactId ? (
                    <Button type="button" variant="ghost" onClick={resetContactForm}>
                      <X size={16} />
                      Hủy
                    </Button>
                  ) : null}
                  <Button type="submit" disabled={savingContact}>
                    {editingContactId ? <Save size={16} /> : <Plus size={16} />}
                    {editingContactId ? 'Lưu liên hệ' : 'Thêm liên hệ'}
                  </Button>
                </div>
              </form>
            ) : null}

            <div className="profile-contact-list">
              {contacts.length ? (
                contacts.map((contact) => (
                  <div className="profile-contact-item" key={contact.id}>
                    <div>
                      <strong>{contact.name}</strong>
                      <span>
                        <Mail size={14} />
                        {contact.phone}
                      </span>
                    </div>
                    {!isReadOnly ? (
                      <div className="inline-actions">
                        <button
                          className="icon-button"
                          type="button"
                          aria-label="Sửa liên hệ"
                          onClick={() => startEditContact(contact)}
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          className="icon-button admin-icon-danger"
                          type="button"
                          aria-label="Xóa liên hệ"
                          onClick={() => setDeleteContactTarget(contact)}
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    ) : null}
                  </div>
                ))
              ) : (
                <div className="profile-management-empty">Chưa có liên hệ nào được lưu.</div>
              )}
            </div>
          </div>
        </article>
      </section>

      <ConfirmDialog
        confirmLabel="Xóa liên hệ"
        description={`Bạn có chắc muốn xóa liên hệ "${deleteContactTarget?.name}"?`}
        open={Boolean(deleteContactTarget)}
        title="Xóa liên hệ khẩn cấp"
        onConfirm={handleDeleteContact}
        onOpenChange={(open) => {
          if (!open) {
            setDeleteContactTarget(null)
          }
        }}
      />
    </div>
  )
}

export default ProfileSettingsPage
