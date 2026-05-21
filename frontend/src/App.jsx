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
import { DashboardPage } from './pages/DashboardPage.jsx'
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

function RoleWorkspacePage({ copies, fallback }) {
  const activeRole = useAuthStore((state) => state.activeProfile?.role)
  const copy = copies[activeRole] ?? fallback

  return <WorkspacePage description={copy.description} title={copy.title} />
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
            <Route
              element={
                <RoleWorkspacePage
                  copies={{
                    ELDERLY: {
                      title: 'Thuốc của tôi',
                      description:
                        'Danh sách thuốc cá nhân, thông tin liều dùng và hình ảnh nhận diện thuốc.',
                    },
                    CAREGIVER: {
                      title: 'Quản lý thuốc',
                      description:
                        'Khu vực quản lý thuốc cho người thân, gồm CRUD, upload ảnh và lịch liều uống.',
                    },
                  }}
                  fallback={{
                    title: 'Quản lý thuốc',
                    description: 'Khung module quản lý thuốc đã sẵn sàng để gắn API.',
                  }}
                />
              }
              path="/medications"
            />
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
              <Route
                element={
                  <WorkspacePage
                    description="Báo cáo dùng thuốc, tồn kho và cảnh báo sẽ dùng chart component đã cài sẵn."
                    title="Thống kê"
                  />
                }
                path="/reports"
              />
            </Route>

            <Route
              element={
                <WorkspacePage
                  description="Khu vực chat giữa elderly, caregiver và đội ngũ quản trị khi cần hỗ trợ."
                  title="Chat"
                />
              }
              path="/chat"
            />

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
