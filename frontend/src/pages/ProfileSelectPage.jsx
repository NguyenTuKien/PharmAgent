import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import logo from '../assets/logo.svg'
import title from '../assets/title.svg'
import { useAuthStore } from '../modules/auth/authStore.js'
import { clearOnboardingState } from '../modules/auth/authFacade.js'
import { getToastErrorMessage, notify } from '../lib/toast.js'

function profileName(profile) {
  const name = [profile.firstName, profile.lastName].filter(Boolean).join(' ').trim()
  return name || 'Ho so PharmAgent'
}

function roleLabel(role) {
  const normalized = role?.toUpperCase()
  if (normalized === 'CAREGIVER') return 'Nguoi cham soc'
  if (normalized === 'ELDERLY') return 'Nguoi than'
  if (normalized === 'ADMIN') return 'Quan tri'
  return 'Ho so'
}

export function ProfileSelectPage() {
  const navigate = useNavigate()
  const profiles = useAuthStore((state) => state.profiles)
  const selectProfile = useAuthStore((state) => state.selectProfile)
  const logout = useAuthStore((state) => state.logout)
  const [selectedId, setSelectedId] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const sortedProfiles = useMemo(
    () => [...profiles].sort((a, b) => profileName(a).localeCompare(profileName(b))),
    [profiles],
  )

  const handleSelect = async (profileId) => {
    setSelectedId(profileId)
    setIsLoading(true)
    try {
      await selectProfile(profileId)
      clearOnboardingState()
      navigate('/dashboard')
    } catch (err) {
      const message = getToastErrorMessage(err, 'Không thể chọn hồ sơ. Vui lòng thử lại.')
      notify.error(message, {
        description: 'Phiên đăng nhập có thể đã hết hạn. Vui lòng thử lại.',
      })
    } finally {
      setIsLoading(false)
    }
  }

  const handleLogout = async () => {
    await logout()
    notify.success('Đã đăng xuất', {
      description: 'Phiên đăng nhập đã được xóa khỏi trình duyệt này.',
    })
    navigate('/login')
  }

  return (
    <main className="profile-page">
      <header className="profile-header">
        <div className="brand brand--lockup" style={{ marginBottom: 22 }}>
          <img className="brand-logo" src={logo} alt="" />
          <img className="brand-title" src={title} alt="PharmAgent" />
        </div>
        <p className="eyebrow">Chon ho so</p>
        <h1>Ban muon tiep tuc voi vai tro nao?</h1>
        <p>
          Moi ho so co quyen truy cap rieng de bao ve thong tin thuoc va lich cham soc.
        </p>
      </header>

      <section className="profile-grid">
        {sortedProfiles.map((profile) => (
          <article className="profile-card" key={profile.id}>
            <div className="avatar-circle">
              {profile.avatarUrl ? (
                <img src={profile.avatarUrl} alt="" />
              ) : (
                profileName(profile).slice(0, 1).toUpperCase()
              )}
            </div>
            <div>
              <h2>{profileName(profile)}</h2>
              <p>{roleLabel(profile.role)}</p>
            </div>
            <button
              type="button"
              className="btn btn--primary btn--sm"
              disabled={isLoading}
              onClick={() => handleSelect(profile.id)}
            >
              {isLoading && selectedId === profile.id ? 'Dang chon...' : 'Chon'}
            </button>
          </article>
        ))}
      </section>

      {!sortedProfiles.length && (
        <div className="empty-state compact">
          <h2>Chua co ho so nao</h2>
          <p>Vui long lien he quan tri vien hoac dang ky lai tai khoan.</p>
        </div>
      )}

      <div className="inline-actions" style={{ marginTop: 22 }}>
        <button type="button" className="btn btn--ghost btn--md" onClick={handleLogout}>
          Dang xuat
        </button>
      </div>
    </main>
  )
}

export default ProfileSelectPage
