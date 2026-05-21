import { Outlet } from 'react-router-dom'

import { Topbar } from './Topbar.jsx'

export function AppShell() {
  return (
    <div className="app-shell">
      <div className="app-main">
        <Topbar />
        <main className="content-panel">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
