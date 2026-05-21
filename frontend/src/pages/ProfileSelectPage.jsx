import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ArrowRight,
  HeartPulse,
  LoaderCircle,
  LogOut,
  Phone,
  ShieldCheck,
  User,
  Users,
} from 'lucide-react'

import logo from '../assets/logo.svg'
import title from '../assets/system/title.svg'
import { getToastErrorMessage, notify } from '../lib/toast.js'
import { clearOnboardingState } from '../modules/auth/authFacade.js'
import { useAuthStore } from '../modules/auth/authStore.js'
import { getAutoSelectableProfile, getProfileLandingPath } from '../modules/auth/session.js'
import '../styles/profile-select/profile-select.css'

const rolePriority = {
  ADMIN: 0,
  CAREGIVER: 1,
  ELDERLY: 2,
}

const roleConfig = {
  ADMIN: {
    label: 'Quản trị viên',
    description: 'Quản lý người dùng, danh mục thuốc và cấu hình hệ thống.',
    Icon: ShieldCheck,
    badgeClass: 'bg-slate-900 text-white border-slate-800',
    avatarClass: 'from-slate-800 to-slate-600 text-white',
  },
  CAREGIVER: {
    label: 'Người chăm sóc',
    description: 'Dành cho người chăm sóc, giúp tạo và theo dõi lịch thuốc, quản lý liên lạc với người thân.',
    Icon: Users,
    badgeClass: 'bg-[var(--accent-soft)] text-[var(--accent-strong)] border-[#b8d8cf]',
    avatarClass: 'from-[#1f8a70] to-[#67b99f] text-white',
  },
  ELDERLY: {
    label: 'Người thân',
    description: 'Dành cho người cần được chăm sóc, giúp xem thông tin thuốc và lịch uống, nhắc nhở và thông tin chăm sóc cá nhân.',
    Icon: HeartPulse,
    badgeClass: 'bg-[#fff3dd] text-[#9a5b12] border-[#efd2a2]',
    avatarClass: 'from-[#d89032] to-[#efbd6f] text-white',
  },
}

function normalizeRole(role) {
  return typeof role === 'string' ? role.toUpperCase() : 'PROFILE'
}

function getRoleConfig(role) {
  return roleConfig[normalizeRole(role)] ?? {
    label: 'Hồ sơ',
    description: 'Tiếp tục với quyền truy cập được cấp cho hồ sơ này.',
    Icon: User,
    badgeClass: 'bg-white text-[var(--muted)] border-[var(--border)]',
    avatarClass: 'from-[#dfe8e4] to-[#c9d8d2] text-[var(--accent-strong)]',
  }
}

function profileName(profile) {
  const name = [profile?.firstName, profile?.lastName].filter(Boolean).join(' ').trim()
  return name || 'Hồ sơ PharmAgent'
}

function profileInitials(profile) {
  const parts = profileName(profile).split(/\s+/).filter(Boolean)
  const initials = parts.length > 1 ? `${parts[0][0]}${parts[parts.length - 1][0]}` : parts[0]?.[0]
  return (initials || 'P').toUpperCase()
}

function profilePhone(profile) {
  return profile?.phone || 'Chưa cập nhật số điện thoại'
}

function sortProfiles(profiles) {
  return [...profiles].sort((a, b) => {
    const roleDelta =
      (rolePriority[normalizeRole(a.role)] ?? 9) - (rolePriority[normalizeRole(b.role)] ?? 9)

    if (roleDelta !== 0) {
      return roleDelta
    }

    return profileName(a).localeCompare(profileName(b), 'vi')
  })
}

function ProfileAvatar({ profile }) {
  const config = getRoleConfig(profile?.role)

  return (
    <div
      className={`grid h-[76px] w-[76px] shrink-0 place-items-center overflow-hidden rounded-[1.35rem] bg-gradient-to-br text-2xl font-black shadow-[0_18px_34px_-18px_rgba(22,33,31,0.58)] sm:h-20 sm:w-20 ${config.avatarClass}`}
    >
      {profile?.avatarUrl ? (
        <img className="h-full w-full object-cover" src={profile.avatarUrl} alt="" />
      ) : (
        <span>{profileInitials(profile)}</span>
      )}
    </div>
  )
}

function ProfileCard({ disabled, isSelected, onSelect, profile, style }) {
  const config = getRoleConfig(profile.role)
  const { Icon } = config

  return (
    <button
      type="button"
      className={`profile-select-card group text-left ${isSelected ? 'is-selected' : ''}`}
      style={style}
      disabled={disabled}
      aria-busy={isSelected}
      onClick={() => onSelect(profile.id)}
    >
      <div className="relative z-[1] flex h-full flex-col gap-6">
        <div className="flex items-start justify-between gap-4">
          <ProfileAvatar profile={profile} />
          <span
            className={`inline-flex max-w-full items-center gap-2 rounded-full border px-3.5 py-1.5 text-xs font-extrabold leading-none shadow-[0_10px_24px_-20px_rgba(22,33,31,0.45)] ${config.badgeClass}`}
          >
            <Icon className="h-3.5 w-3.5 shrink-0" strokeWidth={2} />
            <span className="truncate">{config.label}</span>
          </span>
        </div>

        <div className="min-w-0">
          <h2 className="text-[1.45rem] font-black leading-tight tracking-normal text-[var(--text)] sm:text-2xl">
            {profileName(profile)}
          </h2>
          <p className="mt-3 text-[0.95rem] leading-6 text-[var(--muted)]">{config.description}</p>
        </div>

        <div className="mt-auto flex flex-col gap-4 border-t border-[color:rgb(219_228_224/0.86)] pt-5">
          <div className="flex min-w-0 items-center gap-2 text-sm font-bold text-[var(--text)]">
            <Phone className="h-4 w-4 shrink-0 text-[var(--accent)]" strokeWidth={2} />
            <span className="truncate">{profilePhone(profile)}</span>
          </div>

          <div className="flex items-center justify-end gap-2 text-sm font-black text-[var(--accent-strong)]">
            <span>{isSelected ? 'Đang mở hồ sơ...' : 'Tiếp tục'}</span>
            {isSelected ? (
              <LoaderCircle className="h-5 w-5 animate-spin" strokeWidth={2} />
            ) : (
              <ArrowRight
                className="h-5 w-5 transition-transform duration-200 group-hover:translate-x-1"
                strokeWidth={2}
              />
            )}
          </div>
        </div>
      </div>
    </button>
  )
}

function AutoContinueState({ profile }) {
  const config = getRoleConfig(profile?.role)
  const { Icon } = config

  return (
    <main className="profile-select-page relative isolate min-h-[100dvh] overflow-hidden px-4 py-6 text-[var(--text)] sm:px-6 lg:px-10">
      <section className="mx-auto flex min-h-[calc(100dvh-3rem)] w-full max-w-4xl items-center justify-center">
        <div className="profile-select-auto-card w-full rounded-[2rem] border border-[color:var(--border)] bg-white/80 p-6 shadow-[0_30px_80px_-45px_rgba(22,33,31,0.45)] backdrop-blur md:p-9">
          <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
            <ProfileAvatar profile={profile} />
            <div className="min-w-0 flex-1">
              <span
                className={`mb-3 inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-extrabold ${config.badgeClass}`}
              >
                <Icon className="h-3.5 w-3.5" strokeWidth={2} />
                {config.label}
              </span>
              <h1 className="text-2xl font-black leading-tight sm:text-3xl">Đang mở hồ sơ phù hợp</h1>
              <p className="mt-2 text-sm leading-6 text-[var(--muted)]">
                {profileName(profile)} sẽ được đưa thẳng vào hệ thống vì tài khoản này không cần bước chọn hồ sơ.
              </p>
            </div>
            <LoaderCircle
              className="h-8 w-8 shrink-0 animate-spin text-[var(--accent)]"
              strokeWidth={2}
            />
          </div>
          <div className="profile-select-loading-bar mt-7" />
        </div>
      </section>
    </main>
  )
}

export function ProfileSelectPage() {
  const navigate = useNavigate()
  const profiles = useAuthStore((state) => state.profiles)
  const selectProfile = useAuthStore((state) => state.selectProfile)
  const logout = useAuthStore((state) => state.logout)
  const [selectedId, setSelectedId] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [autoSelectFailed, setAutoSelectFailed] = useState(false)

  const sortedProfiles = useMemo(() => sortProfiles(profiles), [profiles])
  const autoProfile = useMemo(() => getAutoSelectableProfile(profiles), [profiles])

  const chooseProfile = useCallback(
    async (profileId, options = {}) => {
      setSelectedId(profileId)
      setIsLoading(true)

      try {
        const activeProfile = await selectProfile(profileId)
        clearOnboardingState()
        navigate(getProfileLandingPath(activeProfile), { replace: true })
      } catch (err) {
        if (options.auto) {
          setAutoSelectFailed(true)
        }

        const message = getToastErrorMessage(err, 'Không thể chọn hồ sơ. Vui lòng thử lại.')
        notify.error(message, {
          description: 'Phiên đăng nhập có thể đã hết hạn. Vui lòng thử lại.',
        })
      } finally {
        setIsLoading(false)
      }
    },
    [navigate, selectProfile],
  )

  useEffect(() => {
    if (!autoProfile || autoSelectFailed || selectedId) {
      return
    }

    chooseProfile(autoProfile.id, { auto: true })
  }, [autoProfile, autoSelectFailed, chooseProfile, selectedId])

  const handleLogout = async () => {
    await logout()
    notify.success('Đã đăng xuất')
    navigate('/login', { replace: true })
  }

  if (autoProfile && !autoSelectFailed) {
    return <AutoContinueState profile={autoProfile} />
  }

  return (
    <main className="profile-select-page relative isolate min-h-[100dvh] overflow-hidden px-4 pb-28 pt-6 text-[var(--text)] sm:px-6 sm:pb-24 lg:px-10">
      <section className="profile-select-shell mx-auto flex min-h-[calc(100dvh-7rem)] w-full max-w-7xl flex-col items-center py-6 sm:py-9 lg:py-12">
        <header className="profile-select-header mx-auto w-full max-w-3xl text-center">
          <div className="brand brand--lockup profile-select-brand mb-6 justify-center">
            <img className="brand-logo" src={logo} alt="" />
            <img className="brand-title" src={title} alt="PharmAgent" />
          </div>

          <h1 className="profile-select-question text-3xl font-black leading-[1.08] tracking-normal text-[var(--text)] sm:text-4xl lg:text-5xl">
            Bạn muốn tiếp tục với hồ sơ nào?
          </h1>
        </header>

        <section className="profile-select-cards-panel mt-9 w-full sm:mt-12" aria-label="Danh sách hồ sơ">
          {sortedProfiles.length ? (
            <div className="profile-select-grid">
              {sortedProfiles.map((profile, index) => (
                <ProfileCard
                  key={profile.id}
                  profile={profile}
                  disabled={isLoading}
                  isSelected={isLoading && selectedId === profile.id}
                  onSelect={chooseProfile}
                  style={{ '--profile-index': index }}
                />
              ))}
            </div>
          ) : (
            <div className="profile-select-empty-card mx-auto max-w-xl rounded-[2rem] border border-[color:var(--border)] bg-white/80 p-6 text-left shadow-[0_30px_80px_-45px_rgba(22,33,31,0.45)] backdrop-blur sm:p-8">
              <div className="grid h-14 w-14 place-items-center rounded-2xl bg-[var(--accent-soft)] text-[var(--accent-strong)]">
                <User className="h-7 w-7" strokeWidth={2} />
              </div>
              <h2 className="mt-5 text-2xl font-black">Chưa có hồ sơ khả dụng</h2>
              <p className="mt-2 text-sm leading-6 text-[var(--muted)]">
                Vui lòng đăng xuất rồi đăng nhập lại, hoặc liên hệ quản trị viên nếu tài khoản đã được tạo.
              </p>
            </div>
          )}
        </section>
      </section>

      <div className="profile-select-logout-wrap">
        <button
          type="button"
          className="profile-select-logout"
          onClick={handleLogout}
          disabled={isLoading}
        >
          <LogOut className="h-4 w-4" strokeWidth={2} />
          Đăng xuất
        </button>
      </div>
    </main>
  )
}

export default ProfileSelectPage
