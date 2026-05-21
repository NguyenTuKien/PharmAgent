import { Navigate, NavLink, Outlet, useLocation } from 'react-router-dom'
import { Pill, Users } from 'lucide-react'

const adminNavItems = [
  { to: '/admin/users', label: 'Tài khoản', icon: Users },
  { to: '/admin/pills', label: 'Thư viện thuốc', icon: Pill },
]

export function AdminLayout() {
  const location = useLocation()
  const isRoot = location.pathname === '/admin' || location.pathname === '/admin/'

  if (isRoot) {
    return <Navigate replace to="/admin/users" />
  }

  return (
    <div className="admin-layout">
      <div className="admin-content">
        <Outlet />
      </div>
    </div>
  )
}
