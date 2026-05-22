import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useAuthStore } from '../modules/auth/authStore.js'
import { canAccessRoles, getProfileLandingPath } from '../modules/auth/session.js'

function RestoringSession() {
  return (
    <div className="empty-state compact" role="status">
      <h2>Dang khoi phuc phien</h2>
      <p>Vui long doi trong giay lat.</p>
    </div>
  )
}

export function GuestRoute() {
  const location = useLocation()
  const accessToken = useAuthStore((state) => state.accessToken)
  const activeProfile = useAuthStore((state) => state.activeProfile)
  const authToken = useAuthStore((state) => state.authToken)
  const status = useAuthStore((state) => state.status)

  if (status === 'restoring') {
    return <RestoringSession />
  }

  if (accessToken && activeProfile) {
    return <Navigate replace to={location.state?.from?.pathname ?? getProfileLandingPath(activeProfile)} />
  }

  if (authToken) {
    return <Navigate replace to="/profiles" />
  }

  return <Outlet />
}

export function ProfileSelectionRoute() {
  const accessToken = useAuthStore((state) => state.accessToken)
  const activeProfile = useAuthStore((state) => state.activeProfile)
  const authToken = useAuthStore((state) => state.authToken)
  const refreshToken = useAuthStore((state) => state.refreshToken)
  const status = useAuthStore((state) => state.status)

  if (status === 'restoring') {
    return <RestoringSession />
  }

  if (accessToken && activeProfile) {
    return <Navigate replace to={getProfileLandingPath(activeProfile)} />
  }

  if (!authToken && !refreshToken) {
    return <Navigate replace to="/login" />
  }

  return <Outlet />
}

export function ProtectedRoute() {
  const location = useLocation()
  const accessToken = useAuthStore((state) => state.accessToken)
  const activeProfile = useAuthStore((state) => state.activeProfile)
  const authToken = useAuthStore((state) => state.authToken)
  const refreshToken = useAuthStore((state) => state.refreshToken)
  const status = useAuthStore((state) => state.status)

  if (status === 'restoring' || (!accessToken && activeProfile && refreshToken)) {
    return <RestoringSession />
  }

  if (!accessToken || !activeProfile) {
    return (
      <Navigate
        replace
        state={{ from: location }}
        to={authToken ? '/profiles' : '/login'}
      />
    )
  }

  return <Outlet />
}

export function RoleRoute({ roles }) {
  const activeRole = useAuthStore((state) => state.activeProfile?.role)

  if (!canAccessRoles(activeRole, roles)) {
    return <Navigate replace to="/unauthorized" />
  }

  return <Outlet />
}
