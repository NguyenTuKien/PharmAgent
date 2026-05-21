import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const projectRoot = dirname(dirname(fileURLToPath(import.meta.url)))
const profileSettingsSource = readFileSync(join(projectRoot, 'pages', 'ProfileSettingsPage.jsx'), 'utf8')

test('profile settings uses upload and camera avatar controls instead of URL entry', () => {
  assert.match(profileSettingsSource, /uploadImageToCloudinary/)
  assert.match(profileSettingsSource, /updateMyAvatar/)
  assert.match(profileSettingsSource, /CameraCapture/)
  assert.match(profileSettingsSource, /Ảnh đại diện/)
  assert.doesNotMatch(profileSettingsSource, /Avatar URL|Đường dẫn ảnh đại diện/)
})

test('profile settings renders a person placeholder when avatar is missing', () => {
  assert.match(profileSettingsSource, /UserRound/)
  assert.doesNotMatch(profileSettingsSource, /profileInitials/)
})

test('profile settings is composed with Tailwind information cards', () => {
  assert.match(profileSettingsSource, /max-w-\[1480px\]/)
  assert.match(profileSettingsSource, /rounded-lg border border-slate-200 bg-white/)
  assert.doesNotMatch(profileSettingsSource, /profile-management-page|profile-management-panel|profile-management-form/)
})

test('profile settings replaces emergency contacts with device management', () => {
  assert.match(profileSettingsSource, /Quản lý thiết bị/)
  assert.match(profileSettingsSource, /getMyDevices/)
  assert.match(profileSettingsSource, /addMyDevice/)
  assert.match(profileSettingsSource, /updateMyDevice/)
  assert.match(profileSettingsSource, /deleteMyDevice/)
  assert.doesNotMatch(profileSettingsSource, /Danh bạ khẩn cấp|ContactCard|EMPTY_CONTACT_FORM|getMyContacts/)
})
