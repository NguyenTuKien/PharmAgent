import { ArrowRight, UserRound } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'

import { Button } from '../components/ui/Button.jsx'
import { getApiErrorMessage } from '../lib/apiClient.js'
import { useAuthStore } from '../modules/auth/authStore.js'

function profileLabel(profile) {
  return [profile.firstName, profile.lastName].filter(Boolean).join(' ') || profile.id
}

export function ProfileSelectPage() {
  const navigate = useNavigate()
  const { profiles, selectProfile } = useAuthStore((state) => ({
    profiles: state.profiles,
    selectProfile: state.selectProfile,
  }))

  const handleSelect = async (profileId) => {
    try {
      await selectProfile(profileId)
      toast.success('Da chon ho so')
      navigate('/dashboard', { replace: true })
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    }
  }

  return (
    <main className="profile-page">
      <section className="profile-header">
        <p className="eyebrow">Token flow</p>
        <h1>Chon ho so su dung</h1>
        <p>
          Login tra ve authToken va refreshToken. Buoc nay goi endpoint select profile de lay
          accessToken theo role cua ho so.
        </p>
      </section>

      <section className="profile-grid" aria-label="Danh sach ho so">
        {profiles.length ? (
          profiles.map((profile) => (
            <article className="profile-card" key={profile.id}>
              <div className="avatar-circle">
                {profile.avatarUrl ? (
                  <img alt="" src={profile.avatarUrl} />
                ) : (
                  <UserRound aria-hidden size={26} />
                )}
              </div>
              <div>
                <h2>{profileLabel(profile)}</h2>
                <p>{profile.role || 'PROFILE'}</p>
              </div>
              <Button variant="secondary" onClick={() => handleSelect(profile.id)}>
                Chon
                <ArrowRight size={18} />
              </Button>
            </article>
          ))
        ) : (
          <div className="empty-state">
            <h2>Chua co ho so trong phien nay</h2>
            <p>Hay dang nhap lai de tai danh sach ho so tu backend.</p>
            <Button onClick={() => navigate('/login')}>Quay lai dang nhap</Button>
          </div>
        )}
      </section>
    </main>
  )
}
