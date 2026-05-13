import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useAuthStore } from '../modules/auth/authStore.js'
import { canAccessRoles } from '../modules/auth/session.js'

export function GuestRoute() {
  const location = useLocation()
  const { accessToken, activeProfile, authToken } = useAuthStore((state) => ({
    accessToken: state.accessToken,
    activeProfile: state.activeProfile,
    authToken: state.authToken,
  }))

  if (accessToken && activeProfile) {
    return <Navigate replace to={location.state?.from?.pathname ?? '/dashboard'} />
  }

  if (authToken) {
    return <Navigate replace to="/profiles" />
  }

  return <Outlet />
}

export function ProfileSelectionRoute() {
  const { accessToken, activeProfile, authToken, refreshToken } = useAuthStore((state) => ({
    accessToken: state.accessToken,
    activeProfile: state.activeProfile,
    authToken: state.authToken,
    refreshToken: state.refreshToken,
  }))

  if (accessToken && activeProfile) {
    return <Navigate replace to="/dashboard" />
  }

  if (!authToken && !refreshToken) {
    return <Navigate replace to="/login" />
  }

  return <Outlet />
}

export function ProtectedRoute() {
  const location = useLocation()
  const { accessToken, activeProfile, authToken } = useAuthStore((state) => ({
    accessToken: state.accessToken,
    activeProfile: state.activeProfile,
    authToken: state.authToken,
  }))

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
