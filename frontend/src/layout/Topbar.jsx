import { LogOut, UserRound } from 'lucide-react'
import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { gooeyToast } from 'goey-toast'

import { Button } from '../components/ui/Button.jsx'
import { GooeySearchTabs } from '../components/ui/GooeySearchTabs.jsx'
import { ConfirmDialog } from '../components/ui/Modal.jsx'
import { useAuthStore } from '../modules/auth/authStore.js'
import { canAccessRoles } from '../modules/auth/session.js'
import { navigationItems } from './navigation.js'

function getProfileName(profile) {
  const name = [profile?.firstName, profile?.lastName].filter(Boolean).join(' ')
  return name || profile?.id || 'Chua chon ho so'
}

export function Topbar() {
  const navigate = useNavigate()
  const location = useLocation()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const { activeProfile, logout } = useAuthStore((state) => ({
    activeProfile: state.activeProfile,
    logout: state.logout,
  }))
  const visibleItems = navigationItems.filter((item) =>
    canAccessRoles(activeProfile?.role, item.roles ?? []),
  )
  const activeTab =
    visibleItems.find((item) => location.pathname.startsWith(item.to))?.to ?? visibleItems[0]?.to
  const searchTabs = visibleItems.map((item) => {
    const Icon = item.icon
    return {
      label: item.label,
      value: item.to,
      icon: <Icon size={15} />,
    }
  })

  const handleLogout = async () => {
    await logout()
    gooeyToast.success('Da dang xuat')
    navigate('/login', { replace: true })
  }

  const handleTabChange = (value) => {
    const target = visibleItems.find((item) => item.to === value)
    if (target) {
      navigate(target.to)
    }
  }

  const handleSearch = (value) => {
    const params = new URLSearchParams(location.search)
    const query = value.trim()

    if (query) {
      params.set('q', query)
    } else {
      params.delete('q')
    }

    const search = params.toString()
    navigate(`${location.pathname}${search ? `?${search}` : ''}`)
  }

  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Ho so dang dung</p>
        <h1>{getProfileName(activeProfile)}</h1>
      </div>
      <GooeySearchTabs
        activeTab={activeTab}
        placeholder="Tim trong PharmAgent..."
        preset="smooth"
        tabs={searchTabs}
        onSearch={handleSearch}
        onTabChange={handleTabChange}
      />
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
