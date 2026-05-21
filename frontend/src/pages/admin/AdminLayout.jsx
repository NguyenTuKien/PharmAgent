import { Outlet } from 'react-router-dom'

export function AdminLayout() {
  return (
    <div className="admin-layout">
      <div className="admin-content">
        <Outlet />
      </div>
    </div>
  )
}
