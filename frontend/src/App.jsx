import { useEffect } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'

import { AppShell } from './layout/AppShell.jsx'
import { useAuthStore } from './modules/auth/authStore.js'
import { SESSION_STORAGE_KEY } from './modules/auth/session.js'
import { ActiveSessionsPage } from './pages/admin/ActiveSessionsPage.jsx'
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage.jsx'
import { AdminLayout } from './pages/admin/AdminLayout.jsx'
import { AdminPillsPage } from './pages/admin/AdminPillsPage.jsx'
import { AdminUsersPage } from './pages/admin/AdminUsersPage.jsx'
import { ChatPage } from './pages/ChatPage.jsx'
import { DashboardPage } from './pages/DashboardPage.jsx'
import { MedicationsPage } from './pages/MedicationsPage.jsx'
import { ForgotPasswordPage } from './pages/auth/ForgotPasswordPage.jsx'
import { ElderlySetupPage } from './pages/auth/ElderlySetupPage.jsx'
import { LoginPage } from './pages/auth/LoginPage.jsx'
import { RegisterPage } from './pages/auth/RegisterPage.jsx'
import { ResetPasswordPage } from './pages/auth/ResetPasswordPage.jsx'
import { VerifyEmailPage } from './pages/auth/VerifyEmailPage.jsx'
import { NotFoundPage } from './pages/NotFoundPage.jsx'
import { ProfileSettingsPage } from './pages/ProfileSettingsPage.jsx'
import { ProfileSelectPage } from './pages/ProfileSelectPage.jsx'
import { RelationshipsPage } from './pages/caregiver/RelationshipsPage.jsx'
import { ReportsPage } from './pages/caregiver/ReportsPage.jsx'
import { ScanPage } from './pages/ScanPage.jsx'
import { UnauthorizedPage } from './pages/UnauthorizedPage.jsx'
import { WorkspacePage } from './pages/WorkspacePage.jsx'
import {
  GuestRoute,
  ProfileSelectionRoute,
  ProtectedRoute,
  RoleRoute,
} from './routes/guards.jsx'

function SessionBootstrap() {
  const accessToken = useAuthStore((state) => state.accessToken)
  const activeProfileId = useAuthStore((state) => state.activeProfile?.id)
  const refreshSession = useAuthStore((state) => state.refreshSession)
  const refreshToken = useAuthStore((state) => state.refreshToken)
  const syncLocalSession = useAuthStore((state) => state.syncLocalSession)

  useEffect(() => {
    if (!accessToken && refreshToken && activeProfileId) {
      refreshSession().catch(() => undefined)
    }
  }, [accessToken, activeProfileId, refreshSession, refreshToken])

  useEffect(() => {
    const handleStorage = (event) => {
      if (event.key === SESSION_STORAGE_KEY) {
        syncLocalSession()
      }
    }

    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [syncLocalSession])

  return null
}

function App() {
  return (
    <>
      <SessionBootstrap />
      <Routes>
        <Route element={<GuestRoute />}>
          <Route element={<LoginPage />} path="/login" />
          <Route element={<RegisterPage />} path="/register" />
          <Route element={<ElderlySetupPage />} path="/register/elderly" />
          <Route element={<ForgotPasswordPage />} path="/forgot-password" />
          <Route element={<ResetPasswordPage />} path="/reset-password" />
          <Route element={<VerifyEmailPage />} path="/verify-email" />
        </Route>

        <Route element={<ProfileSelectionRoute />}>
          <Route element={<ProfileSelectPage />} path="/profiles" />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route element={<Navigate replace to="/dashboard" />} index />
            <Route element={<DashboardPage />} path="/dashboard" />
            <Route element={<MedicationsPage />} path="/medications" />
            <Route
              element={
                <WorkspacePage
                  description="Lưu lại các lần uống thuốc, trạng thái xác nhận và ghi chú bất thường."
                  title="Lịch sử uống thuốc"
                />
              }
              path="/dose-history"
            />
            <Route element={<ScanPage />} path="/scan" />
            <Route element={<RoleRoute roles={['ELDERLY', 'CAREGIVER']} />}>
              <Route element={<RelationshipsPage />} path="/relationships" />
            </Route>

            <Route element={<RoleRoute roles={['CAREGIVER', 'ADMIN']} />}>
              <Route element={<ReportsPage />} path="/reports" />
            </Route>

            <Route element={<RoleRoute roles={['ELDERLY', 'CAREGIVER']} />}>
              <Route element={<ChatPage />} path="/chat" />
              <Route element={<ChatPage />} path="/chat/:roomId" />
            </Route>

            <Route element={<ProfileSettingsPage />} path="/my-profile" />

            <Route element={<RoleRoute roles={['ADMIN']} />}>
              <Route element={<AdminLayout />} path="/admin">
                <Route index element={<AdminDashboardPage />} />
                <Route element={<AdminUsersPage />} path="users" />
                <Route element={<AdminPillsPage />} path="pills" />
                <Route element={<ActiveSessionsPage />} path="sessions" />
              </Route>
            </Route>

            <Route element={<UnauthorizedPage />} path="/unauthorized" />
          </Route>
        </Route>

        <Route element={<NotFoundPage />} path="*" />
      </Routes>
    </>
  )
}

export default App
