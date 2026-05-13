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
  },
  {
    to: '/medications',
    label: 'Don thuoc',
    icon: Pill,
  },
  {
    to: '/scan',
    label: 'Quet thuoc',
    icon: Camera,
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
    to: '/admin',
    label: 'Quan tri',
    icon: ShieldCheck,
    roles: ['ADMIN'],
  },
]
