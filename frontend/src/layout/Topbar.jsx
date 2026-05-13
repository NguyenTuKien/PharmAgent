import { LogOut, UserRound } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'

import { Button } from '../components/ui/Button.jsx'
import { ConfirmDialog } from '../components/ui/Modal.jsx'
import { useAuthStore } from '../modules/auth/authStore.js'

function getProfileName(profile) {
  const name = [profile?.firstName, profile?.lastName].filter(Boolean).join(' ')
  return name || profile?.id || 'Chua chon ho so'
}

export function Topbar() {
  const navigate = useNavigate()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const { activeProfile, logout } = useAuthStore((state) => ({
    activeProfile: state.activeProfile,
    logout: state.logout,
  }))

  const handleLogout = async () => {
    await logout()
    toast.success('Da dang xuat')
    navigate('/login', { replace: true })
  }

  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Ho so dang dung</p>
        <h1>{getProfileName(activeProfile)}</h1>
      </div>
      <div className="topbar-actions">
        <Button variant="secondary" onClick={() => navigate('/profiles')}>
          <UserRound size={18} />
          Doi ho so
        </Button>
        <Button aria-label="Dang xuat" variant="ghost" onClick={() => setConfirmOpen(true)}>
          <LogOut size={18} />
        </Button>
      </div>
      <ConfirmDialog
        confirmLabel="Dang xuat"
        description="Phien lam viec hien tai se duoc xoa khoi trinh duyet nay."
        open={confirmOpen}
        title="Dang xuat khoi PharmAgent?"
        onConfirm={handleLogout}
        onOpenChange={setConfirmOpen}
      />
    </header>
  )
}
