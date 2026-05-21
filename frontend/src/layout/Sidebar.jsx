import { NavLink } from 'react-router-dom'

import logo from '../assets/logo.svg'
import title from '../assets/title.svg'
import { useAuthStore } from '../modules/auth/authStore.js'
import { canAccessRoles } from '../modules/auth/session.js'
import { navigationItems } from './navigation.js'

export function Sidebar() {
  const activeRole = useAuthStore((state) => state.activeProfile?.role)
  const visibleItems = navigationItems.filter((item) => canAccessRoles(activeRole, item.roles ?? []))

  return (
    <aside className="sidebar">
      <div className="brand brand--lockup">
        <img className="brand-logo" src={logo} alt="" />
        <img className="brand-title" src={title} alt="PharmAgent" />
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
