import { NavLink } from 'react-router-dom'

import { useAuthStore } from '../modules/auth/authStore.js'
import { canAccessRoles } from '../modules/auth/session.js'
import { navigationItems } from './navigation.js'

export function Sidebar() {
  const activeRole = useAuthStore((state) => state.activeProfile?.role)
  const visibleItems = navigationItems.filter((item) => canAccessRoles(activeRole, item.roles ?? []))

  return (
    <aside className="sidebar">
      <div className="brand">
        <span className="brand-mark">P</span>
        <div>
          <strong>PharmAgent</strong>
          <span>Medication care</span>
        </div>
      </div>
      <nav aria-label="Dieu huong chinh" className="side-nav">
        {visibleItems.map((item) => {
          const Icon = item.icon
          return (
            <NavLink className="nav-link" key={item.to} to={item.to}>
              <Icon size={19} />
              <span>{item.label}</span>
            </NavLink>
          )
        })}
      </nav>
    </aside>
  )
}
