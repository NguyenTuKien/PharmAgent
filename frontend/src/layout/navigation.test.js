import test from 'node:test'
import assert from 'node:assert/strict'

import { getHeaderUtilityItems, getNavigationItemsForRole } from './navigation.js'

const labelsForRole = (role) => getNavigationItemsForRole(role).map((item) => item.label)

test('returns elderly navigation items only', () => {
  assert.deepEqual(labelsForRole('ELDERLY'), [
    'Lịch uống thuốc',
    'Lịch sử uống thuốc',
    'Thuốc của tôi',
    'Người chăm sóc',
  ])
})

test('returns caregiver navigation items with reports', () => {
  assert.deepEqual(labelsForRole('CAREGIVER'), [
    'Người thân',
    'Quản lý thuốc',
    'Lịch uống',
    'Thống kê',
  ])
})

test('returns admin navigation items only', () => {
  assert.deepEqual(labelsForRole('ADMIN'), [
    'Dashboard',
    'Quản lý users',
    'Quản lý thuốc',
    'Quản lý session',
  ])
})

test('returns no navigation items when role is missing', () => {
  assert.deepEqual(labelsForRole(null), [])
})

test('returns header utility icons in the requested order', () => {
  assert.deepEqual(
    getHeaderUtilityItems().map((item) => item.label),
    ['Chat', 'Thông báo', 'Avatar'],
  )
})
