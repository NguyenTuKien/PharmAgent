import {
  ChevronDown,
  Home,
  LogOut,
  UserRound,
} from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

import logo from '../assets/logo.svg'
import title from '../assets/system/title.svg'
import { GooeySearchTabs } from '../components/ui/GooeySearchTabs.jsx'
import { ConfirmDialog } from '../components/ui/Modal.jsx'
import { notify } from '../lib/toast.js'
import { useAuthStore } from '../modules/auth/authStore.js'
import { getProfileLandingPath } from '../modules/auth/session.js'
import { getHeaderUtilityItems, getNavigationItemsForRole } from './navigation.js'

const ROLE_LABELS = {
  ADMIN: 'Quản trị',
  CAREGIVER: 'Người chăm sóc',
  ELDERLY: 'Người dùng',
}

function getProfileName(profile) {
  const name = [profile?.firstName, profile?.lastName].filter(Boolean).join(' ')
  return name || 'Hồ sơ PharmAgent'
}

function getProfileInitials(profile) {
  const first = profile?.firstName?.trim()?.[0]
  const last = profile?.lastName?.trim()?.[0]
  return [first, last].filter(Boolean).join('').toUpperCase() || 'PA'
}

function getRoleLabel(role) {
  return ROLE_LABELS[role?.toUpperCase?.()] ?? 'Hồ sơ'
}

function isNavigationItemActive(pathname, itemPath) {
  return pathname === itemPath || pathname.startsWith(`${itemPath}/`)
}

function RoleGooeyNavigation({ activePath, items, onNavigate, onSearch }) {
  const activeItem = items.find((item) => isNavigationItemActive(activePath, item.to)) ?? items[0]
  const activeNavigationPath = activeItem?.to ?? ''
  const roleNavigationTabs = useMemo(
    () =>
      items.map((item) => {
        const Icon = item.icon

        return {
          value: item.to,
          label: <span className="role-nav-label">{item.label}</span>,
          icon: <Icon aria-hidden="true" size={20} strokeWidth={2.2} />,
        }
      }),
    [items],
  )

  if (!items.length) {
    return null
  }

  return (
    <GooeySearchTabs
      activeTab={activeNavigationPath}
      className="header-gooey-search"
      classNames={{
        activeTab: 'is-active',
        closeButton: 'role-gooey-close',
        container: 'role-gooey-container',
        input: 'role-gooey-input',
        searchButton: 'role-gooey-search-button',
        tab: 'role-nav-link',
        tabList: 'role-nav',
      }}
      gooey
      gooeyIntensity={0.42}
      placeholder="Tìm trong PharmAgent..."
      preset="smooth"
      tabs={roleNavigationTabs}
      onSearch={onSearch}
      onTabChange={(path) => onNavigate(path)}
    />
  )
}

function NotificationMenu() {
  const NotificationIcon = getHeaderUtilityItems()[1].icon

  return (
    <div className="header-popover header-popover--wide" role="menu">
      <div className="popover-heading">
        <strong>Thông báo</strong>
        <span>0 mới</span>
      </div>
      <div className="notification-empty">
        <NotificationIcon size={20} />
        <p>Chưa có thông báo mới.</p>
      </div>
    </div>
  )
}

function AvatarMenu({ activeProfile, onGoHome, onLogout, onSwitchProfile }) {
  return (
    <div className="header-popover header-popover--profile" role="menu">
      <div className="profile-menu-card">
        <div className="avatar-circle avatar-circle--menu">
          {activeProfile?.avatarUrl ? (
            <img src={activeProfile.avatarUrl} alt="" />
          ) : (
            <span>{getProfileInitials(activeProfile)}</span>
          )}
        </div>
        <div>
          <strong>{getProfileName(activeProfile)}</strong>
          <span>{getRoleLabel(activeProfile?.role)}</span>
        </div>
      </div>
      <button className="popover-row" role="menuitem" type="button" onClick={onGoHome}>
        <Home size={17} />
        <span>Trang chính</span>
      </button>
      <button className="popover-row" role="menuitem" type="button" onClick={onSwitchProfile}>
        <UserRound size={17} />
        <span>Đổi hồ sơ</span>
      </button>
      <button className="popover-row popover-row--danger" role="menuitem" type="button" onClick={onLogout}>
        <LogOut size={17} />
        <span>Đăng xuất</span>
      </button>
    </div>
  )
}

export function Topbar() {
  const navigate = useNavigate()
  const location = useLocation()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [openMenu, setOpenMenu] = useState(null)
  const actionsRef = useRef(null)
  const activeProfile = useAuthStore((state) => state.activeProfile)
  const logout = useAuthStore((state) => state.logout)
  const visibleItems = useMemo(
    () => getNavigationItemsForRole(activeProfile?.role),
    [activeProfile?.role],
  )
  const utilityItems = useMemo(() => getHeaderUtilityItems(), [])
  const chatAction = utilityItems.find((item) => item.key === 'chat')
  const notificationAction = utilityItems.find((item) => item.key === 'notifications')
  const profileAction = utilityItems.find((item) => item.key === 'profile')
  const ChatIcon = chatAction.icon
  const NotificationIcon = notificationAction.icon

  useEffect(() => {
    if (!openMenu) {
      return undefined
    }

    const handlePointerDown = (event) => {
      if (actionsRef.current?.contains(event.target)) {
        return
      }
      setOpenMenu(null)
    }

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setOpenMenu(null)
      }
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)

    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [openMenu])

  const handleLogout = async () => {
    await logout()
    notify.success('Đã đăng xuất')
    navigate('/login', { replace: true })
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

  const goHome = () => {
    setOpenMenu(null)
    navigate(getProfileLandingPath(activeProfile))
  }

  const openChat = () => {
    setOpenMenu(null)
    navigate(chatAction.to)
  }

  const switchProfile = () => {
    setOpenMenu(null)
    navigate('/profiles')
  }

  const toggleMenu = (menu) => {
    setOpenMenu((current) => (current === menu ? null : menu))
  }

  const requestLogout = () => {
    setOpenMenu(null)
    setConfirmOpen(true)
  }

  return (
    <>
      <header className="topbar">
        <div className="topbar-main">
          <button className="header-brand" type="button" onClick={goHome}>
            <img className="header-brand-logo" src={logo} alt="" />
            <img className="header-brand-title" src={title} alt="PharmAgent" />
          </button>

          <div className="role-nav-wrap role-nav-wrap--desktop">
            <RoleGooeyNavigation
              activePath={location.pathname}
              items={visibleItems}
              onNavigate={(path) => navigate(path)}
              onSearch={handleSearch}
            />
          </div>

          <div className="header-utilities" ref={actionsRef}>
            <div className="header-actions">
              <button
                aria-label={chatAction.label}
                className="header-action-btn"
                type="button"
                onClick={openChat}
              >
                <ChatIcon size={20} />
              </button>

              <div className="header-menu-anchor">
                <button
                  aria-expanded={openMenu === 'notifications'}
                  aria-haspopup="menu"
                  aria-label={notificationAction.label}
                  className="header-action-btn"
                  type="button"
                  onClick={() => toggleMenu('notifications')}
                >
                  <NotificationIcon size={20} />
                </button>
                {openMenu === 'notifications' ? <NotificationMenu /> : null}
              </div>

              <div className="header-menu-anchor">
                <button
                  aria-expanded={openMenu === 'profile'}
                  aria-haspopup="menu"
                  aria-label={profileAction.label}
                  className="avatar-button"
                  type="button"
                  onClick={() => toggleMenu('profile')}
                >
                  <span className="avatar-circle avatar-circle--header">
                    {activeProfile?.avatarUrl ? (
                      <img src={activeProfile.avatarUrl} alt="" />
                    ) : (
                      <span>{getProfileInitials(activeProfile)}</span>
                    )}
                  </span>
                  <ChevronDown size={15} />
                </button>
                {openMenu === 'profile' ? (
                  <AvatarMenu
                    activeProfile={activeProfile}
                    onGoHome={goHome}
                    onLogout={requestLogout}
                    onSwitchProfile={switchProfile}
                  />
                ) : null}
              </div>
            </div>
          </div>
        </div>

        <ConfirmDialog
          confirmLabel="Đăng xuất"
          description="Phiên làm việc hiện tại sẽ được xóa khỏi trình duyệt này."
          open={confirmOpen}
          title="Đăng xuất khỏi PharmAgent?"
          onConfirm={handleLogout}
          onOpenChange={setConfirmOpen}
        />
      </header>

      <div className="role-nav-wrap role-nav-wrap--mobile">
        <RoleGooeyNavigation
          activePath={location.pathname}
          items={visibleItems}
          onNavigate={(path) => navigate(path)}
          onSearch={handleSearch}
        />
      </div>
    </>
  )
}
