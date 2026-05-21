import {
  BarChart3,
  Bell,
  CalendarClock,
  CalendarDays,
  ClipboardList,
  History,
  LayoutDashboard,
  MessageCircle,
  Pill,
  ScanSearch,
  Settings,
  UserCog,
  UserRound,
  Users,
} from 'lucide-react'

import { canAccessRoles } from '../modules/auth/session.js'

export const navigationItems = [
  {
    to: '/dashboard',
    label: 'Lịch uống thuốc',
    icon: CalendarClock,
    roles: ['ELDERLY'],
  },
  {
    to: '/dose-history',
    label: 'Lịch sử uống thuốc',
    icon: History,
    roles: ['ELDERLY'],
  },
  {
    to: '/medications',
    label: 'Thuốc của tôi',
    icon: Pill,
    roles: ['ELDERLY'],
  },
  {
    to: '/scan',
    label: 'Tìm thuốc',
    icon: ScanSearch,
    roles: ['ELDERLY'],
  },
  {
    to: '/relationships',
    label: 'Người chăm sóc',
    icon: Users,
    roles: ['ELDERLY'],
  },
  {
    to: '/relationships',
    label: 'Người thân',
    icon: Users,
    roles: ['CAREGIVER'],
  },
  {
    to: '/medications',
    label: 'Quản lý thuốc',
    icon: ClipboardList,
    roles: ['CAREGIVER'],
  },
  {
    to: '/dashboard',
    label: 'Lịch uống',
    icon: CalendarDays,
    roles: ['CAREGIVER'],
  },
  {
    to: '/scan',
    label: 'Tìm thuốc',
    icon: ScanSearch,
    roles: ['CAREGIVER'],
  },
  {
    to: '/reports',
    label: 'Thống kê',
    icon: BarChart3,
    roles: ['CAREGIVER'],
  },
  {
    to: '/admin/dashboard',
    label: 'Dashboard',
    icon: LayoutDashboard,
    roles: ['ADMIN'],
  },
  {
    to: '/admin/users',
    label: 'Quản lý users',
    icon: UserCog,
    roles: ['ADMIN'],
  },
  {
    to: '/admin/pills',
    label: 'Quản lý thuốc',
    icon: Pill,
    roles: ['ADMIN'],
  },
  {
    to: '/scan',
    label: 'Tìm thuốc',
    icon: ScanSearch,
    roles: ['ADMIN'],
  },
  {
    to: '/admin/sessions',
    label: 'Quản lý session',
    icon: Settings,
    roles: ['ADMIN'],
  },
]

export function getNavigationItemsForRole(activeRole) {
  return navigationItems.filter((item) => canAccessRoles(activeRole, item.roles ?? []))
}

export const headerUtilityItems = [
  {
    key: 'chat',
    label: 'Chat',
    icon: MessageCircle,
    to: '/chat',
  },
  {
    key: 'notifications',
    label: 'Thông báo',
    icon: Bell,
  },
  {
    key: 'profile',
    label: 'Avatar',
    icon: UserRound,
  },
]

export function getHeaderUtilityItems() {
  return headerUtilityItems
}
