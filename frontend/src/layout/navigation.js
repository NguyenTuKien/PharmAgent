import {
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
    to: '/reports',
    label: 'Bao cao',
    icon: BarChart3,
    roles: ['CAREGIVER', 'ADMIN'],
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
]
