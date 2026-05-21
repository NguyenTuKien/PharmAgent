import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const projectRoot = dirname(dirname(fileURLToPath(import.meta.url)))
const relationshipsSource = readFileSync(
  join(projectRoot, 'pages', 'caregiver', 'RelationshipsPage.jsx'),
  'utf8',
)

test('caregiver relationships page orders list before overview and removes account container', () => {
  const caregiverLayout = relationshipsSource.slice(relationshipsSource.indexOf('Không gian chăm sóc'))
  const listIndex = caregiverLayout.indexOf('Danh sách kết nối')
  const overviewIndex = caregiverLayout.indexOf('<RelationshipOverview')

  assert.notEqual(listIndex, -1)
  assert.notEqual(overviewIndex, -1)
  assert.ok(listIndex < overviewIndex)
  assert.equal(relationshipsSource.includes('Trong tài khoản'), false)
})

test('caregiver relationships page exposes family relation instead of permission editing or scan navigation', () => {
  assert.match(relationshipsSource, /Quan hệ/)
  assert.doesNotMatch(relationshipsSource, /PermissionSelect|PermissionDrawer|PERMISSION_LABELS|Phân quyền|Quyền truy cập/)
  assert.doesNotMatch(relationshipsSource, /onNavigate\('\/scan'\)|ScanSearch|scan thuốc|>\s*Scan\s*</)
})

test('caregiver relationship profile cards are centered modals, not right drawers', () => {
  assert.match(relationshipsSource, /function CenteredCardShell/)
  assert.match(relationshipsSource, /left-1\/2 top-1\/2/)
  assert.match(relationshipsSource, /-translate-x-1\/2 -translate-y-1\/2/)
  assert.doesNotMatch(relationshipsSource, /fixed bottom-0 right-0 top-0/)
})

test('caregiver profile forms use upload and camera avatar controls instead of URL entry', () => {
  assert.match(relationshipsSource, /uploadImageToCloudinary/)
  assert.match(relationshipsSource, /CameraCapture/)
  assert.match(relationshipsSource, /function AvatarPicker/)
  assert.match(relationshipsSource, /Ảnh đại diện/)
  assert.doesNotMatch(relationshipsSource, /Đường dẫn ảnh đại diện|Avatar URL/)
})

test('caregiver relationships include a centered profile detail card with person placeholder avatars', () => {
  assert.match(relationshipsSource, /function ProfileDetailCard/)
  assert.match(relationshipsSource, /detailTarget/)
  assert.match(relationshipsSource, /UserRound/)
  assert.doesNotMatch(relationshipsSource, /<span>\{initials\(profile\)\}<\/span>/)
})
