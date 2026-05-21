import { Navigate, Outlet, useLocation } from 'react-router-dom'

export function AdminLayout() {
  const location = useLocation()
  const isRoot = location.pathname === '/admin' || location.pathname === '/admin/'

  if (isRoot) {
    return <Navigate replace to="/admin/dashboard" />
  }

  return (
    <div className="admin-layout">
      <div className="admin-content">
        <Outlet />
      </div>
    </div>
  )
}
