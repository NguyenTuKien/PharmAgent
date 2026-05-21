import {
  Activity,
  BarChart3,
  Camera,
  LayoutDashboard,
  Pill,
  ShieldCheck,
  Users,
} from 'lucide-react'

export const navigationItems = [
  {
    to: '/dashboard',
    label: 'Tong quan',
    icon: LayoutDashboard,
    roles: ['ELDERLY', 'CAREGIVER'],
  },
  {
    to: '/medications',
    label: 'Don thuoc',
    icon: Pill,
    roles: ['ELDERLY', 'CAREGIVER'],
  },
  {
    to: '/scan',
    label: 'Quet thuoc',
    icon: Camera,
    roles: ['ELDERLY', 'CAREGIVER'],
  },
  {
    to: '/relationships',
    label: 'Nguoi cham soc',
    icon: Users,
    roles: ['CAREGIVER', 'ELDERLY'],
  },
  {
    to: '/admin',
    label: 'Dashboard',
    icon: LayoutDashboard,
    roles: ['ADMIN'],
    exact: true,
  },
  {
    to: '/admin/users',
    label: 'Tai khoan',
    icon: Users,
    roles: ['ADMIN'],
  },
  {
    to: '/admin/pills',
    label: 'Thu vien thuoc',
    icon: Pill,
    roles: ['ADMIN'],
  },
  {
    to: '/admin/sessions',
    label: 'Quan ly Session',
    icon: ShieldCheck,
    roles: ['ADMIN'],
  },
]
