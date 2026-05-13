import { NavLink } from 'react-router-dom'

import { useAuthStore } from '../modules/auth/authStore.js'
import { canAccessRoles } from '../modules/auth/session.js'
import { navigationItems } from './navigation.js'

export function MobileNav() {
  const activeRole = useAuthStore((state) => state.activeProfile?.role)
  const visibleItems = navigationItems
    .filter((item) => canAccessRoles(activeRole, item.roles ?? []))
    .slice(0, 5)

  return (
    <nav aria-label="Dieu huong di dong" className="mobile-nav">
      {visibleItems.map((item) => {
        const Icon = item.icon
        return (
          <NavLink className="mobile-nav-link" key={item.to} to={item.to}>
            <Icon size={20} />
            <span>{item.label}</span>
          </NavLink>
        )
      })}
    </nav>
  )
}
