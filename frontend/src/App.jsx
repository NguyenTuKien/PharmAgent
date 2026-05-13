import { useEffect } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'

import { AppShell } from './layout/AppShell.jsx'
import { useAuthStore } from './modules/auth/authStore.js'
import { DashboardPage } from './pages/DashboardPage.jsx'
import { LoginPage } from './pages/LoginPage.jsx'
import { NotFoundPage } from './pages/NotFoundPage.jsx'
import { ProfileSelectPage } from './pages/ProfileSelectPage.jsx'
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
  const { accessToken, activeProfile, refreshSession, refreshToken } = useAuthStore((state) => ({
    accessToken: state.accessToken,
    activeProfile: state.activeProfile,
    refreshSession: state.refreshSession,
    refreshToken: state.refreshToken,
  }))

  useEffect(() => {
    if (!accessToken && refreshToken && activeProfile?.id) {
      refreshSession().catch(() => undefined)
    }
  }, [accessToken, activeProfile?.id, refreshSession, refreshToken])

  return null
}

function App() {
  return (
    <>
      <SessionBootstrap />
      <Routes>
        <Route element={<GuestRoute />}>
          <Route element={<LoginPage />} path="/login" />
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
                <WorkspacePage
                  description="Khung module quan ly thuoc da san sang de gan CRUD, upload anh va lich lieu uong."
                  title="Don thuoc"
                />
              }
              path="/medications"
            />
            <Route element={<ScanPage />} path="/scan" />
            <Route
              element={
                <WorkspacePage
                  description="Khu vuc nay se gan API moi quan he caregiver/elderly va quyen truy cap theo profile."
                  title="Nguoi cham soc"
                />
              }
              path="/relationships"
            />

            <Route element={<RoleRoute roles={['CAREGIVER', 'ADMIN']} />}>
              <Route
                element={
                  <WorkspacePage
                    description="Bao cao dung thuoc, ton kho va canh bao se dung chart component da cai san."
                    title="Bao cao"
                  />
                }
                path="/reports"
              />
            </Route>

            <Route element={<RoleRoute roles={['ADMIN']} />}>
              <Route
                element={
                  <WorkspacePage
                    description="Khu vuc quan tri danh cho role ADMIN va cac endpoint /api/admin."
                    title="Quan tri"
                  />
                }
                path="/admin"
              />
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
