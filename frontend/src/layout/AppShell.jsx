import { Outlet } from 'react-router-dom'

import { MobileNav } from './MobileNav.jsx'
import { Sidebar } from './Sidebar.jsx'
import { Topbar } from './Topbar.jsx'

export function AppShell() {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="app-main">
        <Topbar />
        <main className="content-panel">
          <Outlet />
        </main>
      </div>
      <MobileNav />
    </div>
  )
}
