import {
  ChevronDown,
  LogOut,
  Settings,
  UserRound,
  X,
} from 'lucide-react'
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

import logo from '../assets/logo.svg'
import title from '../assets/system/title.svg'
import { GooeySearchTabs } from '../components/ui/GooeySearchTabs.jsx'
import { ConfirmDialog } from '../components/ui/Modal.jsx'
import { notify } from '../lib/toast.js'
import { useAuthStore } from '../modules/auth/authStore.js'
import { getProfileLandingPath } from '../modules/auth/session.js'
import { useNotificationStore } from '../modules/notification/notificationStore.js'
import { getCaregiverRelationships, getElderlyRelationships } from '../modules/profile/profileApi.js'
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

function getRoleLabel(role) {
  return ROLE_LABELS[role?.toUpperCase?.()] ?? 'Hồ sơ'
}

function isNavigationItemActive(pathname, itemPath) {
  return pathname === itemPath || pathname.startsWith(`${itemPath}/`)
}

function RoleGooeyNavigation({ activePath, items, onNavigate, onSearch }) {
  const navRootRef = useRef(null)
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

  useLayoutEffect(() => {
    const navRoot = navRootRef.current

    if (!navRoot) {
      return undefined
    }

    let animationFrame = 0

    const updateActiveIndicator = () => {
      const tabList = navRoot.querySelector('.gooey-search-tabs-tabs-content.role-nav')
      const activeTab = tabList?.querySelector('.role-nav-link.is-active')

      if (!tabList || !activeTab) {
        return
      }

      const listRect = tabList.getBoundingClientRect()
      const tabRect = activeTab.getBoundingClientRect()

      navRoot.style.setProperty('--role-nav-indicator-x', `${tabRect.left - listRect.left}px`)
      navRoot.style.setProperty('--role-nav-indicator-width', `${tabRect.width}px`)
    }

    const scheduleUpdate = () => {
      window.cancelAnimationFrame(animationFrame)
      animationFrame = window.requestAnimationFrame(updateActiveIndicator)
    }

    scheduleUpdate()

    if (typeof ResizeObserver === 'undefined') {
      window.addEventListener('resize', scheduleUpdate)

      return () => {
        window.cancelAnimationFrame(animationFrame)
        window.removeEventListener('resize', scheduleUpdate)
      }
    }

    const resizeObserver = new ResizeObserver(scheduleUpdate)
    resizeObserver.observe(navRoot)

    return () => {
      window.cancelAnimationFrame(animationFrame)
      resizeObserver.disconnect()
    }
  }, [activeNavigationPath, items.length])

  if (!items.length) {
    return null
  }

  return (
    <div className="role-nav-motion-wrap" ref={navRootRef}>
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
    </div>
  )
}

function formatTime(sentAt) {
  if (!sentAt) return ''
  const date = new Date(sentAt)
  return (
    date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) +
    ' ' +
    date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
  )
}

function NotificationMenu({
  notifications,
  sentNotifications,
  loading,
  loadingSent,
  onSendNew,
  unreadCount,
}) {
  const NotificationIcon = getHeaderUtilityItems()[1].icon
  const [activeTab, setActiveTab] = useState('received')

  const currentList = activeTab === 'received' ? notifications : sentNotifications
  const currentLoading = activeTab === 'received' ? loading : loadingSent

  return (
    <div className="header-popover header-popover--wide" role="menu">
      <div className="popover-heading flex items-center justify-between border-b border-slate-100 pb-2">
        <div className="flex items-center gap-2">
          <strong>Thông báo</strong>
          {unreadCount > 0 && (
            <span className="bg-red-500 text-white text-[10px] font-extrabold px-1.5 py-0.5 rounded-full">
              {unreadCount} mới
            </span>
          )}
        </div>
        <button
          onClick={onSendNew}
          className="text-[11px] font-bold text-emerald-600 hover:text-emerald-700 bg-emerald-50 hover:bg-emerald-100 px-2.5 py-1 rounded transition-colors"
        >
          Gửi thông báo +
        </button>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-100 text-xs font-bold text-slate-500 mb-1">
        <button
          onClick={() => setActiveTab('received')}
          className={`flex-1 py-2 text-center border-b-2 transition-colors ${
            activeTab === 'received'
              ? 'border-emerald-500 text-emerald-600'
              : 'border-transparent hover:text-slate-700'
          }`}
        >
          Đã nhận ({notifications.length})
        </button>
        <button
          onClick={() => setActiveTab('sent')}
          className={`flex-1 py-2 text-center border-b-2 transition-colors ${
            activeTab === 'sent'
              ? 'border-emerald-500 text-emerald-600'
              : 'border-transparent hover:text-slate-700'
          }`}
        >
          Đã gửi ({sentNotifications.length})
        </button>
      </div>

      {currentLoading && currentList.length === 0 ? (
        <div className="flex justify-center items-center py-8 text-sm text-slate-400 font-medium">
          Đang tải thông báo...
        </div>
      ) : currentList.length === 0 ? (
        <div className="notification-empty py-8 text-center text-slate-400">
          <NotificationIcon className="mx-auto text-slate-300 mb-2" size={24} />
          <p className="text-sm font-semibold">Chưa có thông báo nào.</p>
        </div>
      ) : (
        <div className="max-h-[260px] overflow-y-auto divide-y divide-slate-100/60">
          {currentList.map((notif) => (
            <div key={notif.id} className="p-3 hover:bg-slate-50/70 transition-colors animate-fade-in">
              <p className="text-sm font-semibold text-slate-800 leading-relaxed whitespace-pre-line">
                {notif.content}
              </p>
              <span className="text-[10px] font-bold text-slate-400 mt-1 block">
                {formatTime(notif.sentAt)}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function SendNotificationDialog({ open, onClose, activeProfile }) {
  const [receivers, setReceivers] = useState([])
  const [selectedReceiverId, setSelectedReceiverId] = useState('')
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const sendNotification = useNotificationStore((state) => state.sendNotification)

  useEffect(() => {
    if (!open || !activeProfile) return

    const loadReceivers = async () => {
      setLoading(true)
      try {
        let list = []
        if (activeProfile.role === 'CAREGIVER') {
          list = await getCaregiverRelationships()
        } else if (activeProfile.role === 'ELDERLY') {
          list = await getElderlyRelationships()
        }
        // Filter relationships that are accepted
        const accepted = list.filter((r) => r.status === 'ACCEPTED')
        setReceivers(accepted)
        if (accepted.length > 0) {
          setSelectedReceiverId(accepted[0].id || accepted[0].profileId || accepted[0].relationshipId)
        }
      } catch (err) {
        notify.error('Không thể tải danh sách liên kết')
      } finally {
        setLoading(false)
      }
    }

    loadReceivers()
  }, [open, activeProfile])

  if (!open) return null

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!selectedReceiverId) {
      notify.error('Vui lòng chọn người nhận')
      return
    }
    if (!content.trim()) {
      notify.error('Nội dung thông báo không được để trống')
      return
    }

    setSubmitting(true)
    try {
      const target = receivers.find(
        (r) => (r.id || r.profileId || r.relationshipId) === selectedReceiverId,
      )
      const receiverId = target?.id || target?.profileId || selectedReceiverId

      await sendNotification({ receiverId, content: content.trim() })
      notify.success('Đã gửi thông báo thành công')
      onClose()
      setContent('')
    } catch (err) {
      notify.error('Không thể gửi thông báo')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <div className="modal-overlay" onClick={onClose} />
      <div aria-modal="true" className="modal-panel" role="dialog">
        <div className="modal-header">
          <h2>Gửi thông báo mới</h2>
          <button aria-label="Đóng" className="icon-button" type="button" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4">
          <div className="mb-4">
            <label className="block text-xs font-bold text-slate-500 mb-1.5 uppercase">
              Người nhận
            </label>
            {loading ? (
              <div className="text-sm text-slate-500 py-2">Đang tải danh sách...</div>
            ) : receivers.length === 0 ? (
              <div className="text-sm text-amber-600 bg-amber-50 p-2.5 rounded-lg font-semibold">
                Chưa có liên kết người thân/người chăm sóc nào được xác nhận.
              </div>
            ) : (
              <select
                value={selectedReceiverId}
                onChange={(e) => setSelectedReceiverId(e.target.value)}
                className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-sm font-bold text-slate-900 outline-none focus:border-emerald-500"
              >
                {receivers.map((r) => {
                  const id = r.id || r.profileId || r.relationshipId
                  const name =
                    [r.firstName, r.lastName].filter(Boolean).join(' ') || 'Liên kết'
                  return (
                    <option key={id} value={id}>
                      {name} ({r.phone || 'Không có sđt'})
                    </option>
                  )
                })}
              </select>
            )}
          </div>

          <div className="mb-4">
            <label className="block text-xs font-bold text-slate-500 mb-1.5 uppercase">
              Nội dung
            </label>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Nhập nội dung thông báo..."
              rows={3}
              className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-bold text-slate-900 outline-none focus:border-emerald-500 resize-none"
            />
          </div>

          <div className="modal-actions flex justify-end gap-2 mt-6">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={submitting || receivers.length === 0}
              className="px-4 py-2 text-sm font-bold text-white bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-300 disabled:cursor-not-allowed rounded-lg transition-colors"
            >
              {submitting ? 'Đang gửi...' : 'Gửi'}
            </button>
          </div>
        </form>
      </div>
    </>
  )
}

function AvatarMenu({ activeProfile, onLogout, onOpenSettings, onSwitchProfile }) {
  return (
    <div className="header-popover header-popover--profile" role="menu">
      <div className="profile-menu-card">
        <div className="avatar-circle avatar-circle--menu">
          {activeProfile?.avatarUrl ? (
            <img src={activeProfile.avatarUrl} alt="" />
          ) : (
            <UserRound size={22} strokeWidth={1.9} />
          )}
        </div>
        <div>
          <strong>{getProfileName(activeProfile)}</strong>
          <span>{getRoleLabel(activeProfile?.role)}</span>
        </div>
      </div>
      <button className="popover-row" role="menuitem" type="button" onClick={onSwitchProfile}>
        <UserRound size={17} />
        <span>Đổi hồ sơ</span>
      </button>
      <button className="popover-row" role="menuitem" type="button" onClick={onOpenSettings}>
        <Settings size={17} />
        <span>Thông tin cá nhân</span>
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
  const deselectProfile = useAuthStore((state) => state.deselectProfile)
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

  const notifications = useNotificationStore((state) => state.notifications)
  const fetchNotifications = useNotificationStore((state) => state.fetchNotifications)
  const notificationsLoading = useNotificationStore((state) => state.loading)

  const sentNotifications = useNotificationStore((state) => state.sentNotifications)
  const fetchSentNotifications = useNotificationStore((state) => state.fetchSentNotifications)
  const loadingSent = useNotificationStore((state) => state.loadingSent)

  const [sendNotifOpen, setSendNotifOpen] = useState(false)
  const [lastSeenTime, setLastSeenTime] = useState(() => {
    return localStorage.getItem('last_seen_notif_time') || new Date(0).toISOString()
  })

  useEffect(() => {
    if (!activeProfile) return

    fetchNotifications()
    fetchSentNotifications()
    const interval = setInterval(() => {
      fetchNotifications()
      fetchSentNotifications()
    }, 10000)

    return () => clearInterval(interval)
  }, [activeProfile, fetchNotifications, fetchSentNotifications])

  const unreadCount = useMemo(() => {
    return notifications.filter((n) => n.sentAt > lastSeenTime).length
  }, [notifications, lastSeenTime])

  const handleToggleNotifications = () => {
    if (openMenu === 'notifications') {
      setOpenMenu(null)
    } else {
      setOpenMenu('notifications')
      const nowStr = new Date().toISOString()
      localStorage.setItem('last_seen_notif_time', nowStr)
      setLastSeenTime(nowStr)
    }
  }

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
    deselectProfile()
    navigate('/profiles')
  }

  const openSettings = () => {
    setOpenMenu(null)
    navigate('/my-profile')
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
                  className="header-action-btn relative"
                  type="button"
                  onClick={handleToggleNotifications}
                >
                  <NotificationIcon size={20} />
                  {unreadCount > 0 && (
                    <span className="absolute -top-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[9px] font-black text-white ring-2 ring-white">
                      {unreadCount}
                    </span>
                  )}
                </button>
                {openMenu === 'notifications' ? (
                  <NotificationMenu
                    notifications={notifications}
                    sentNotifications={sentNotifications}
                    loading={notificationsLoading}
                    loadingSent={loadingSent}
                    unreadCount={unreadCount}
                    onSendNew={() => {
                      setOpenMenu(null)
                      setSendNotifOpen(true)
                    }}
                  />
                ) : null}
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
                      <UserRound size={18} strokeWidth={1.9} />
                    )}
                  </span>
                  <ChevronDown size={15} />
                </button>
                {openMenu === 'profile' ? (
                  <AvatarMenu
                    activeProfile={activeProfile}
                    onLogout={requestLogout}
                    onOpenSettings={openSettings}
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

      <SendNotificationDialog
        open={sendNotifOpen}
        onClose={() => setSendNotifOpen(false)}
        activeProfile={activeProfile}
      />
    </>
  )
}
