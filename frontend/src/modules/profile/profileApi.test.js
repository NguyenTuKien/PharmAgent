import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const moduleRoot = dirname(fileURLToPath(import.meta.url))
const profileApiSource = readFileSync(join(moduleRoot, 'profileApi.js'), 'utf8')

test('profile api exposes current user device management endpoints', () => {
  assert.match(profileApiSource, /function getMyDevices/)
  assert.match(profileApiSource, /function addMyDevice/)
  assert.match(profileApiSource, /function updateMyDevice/)
  assert.match(profileApiSource, /function deleteMyDevice/)
  assert.match(profileApiSource, /apiClient\.get\('\/devices\/me'\)/)
  assert.match(profileApiSource, /apiClient\.post\('\/devices\/me', data\)/)
  assert.match(profileApiSource, /apiClient\.put\(`\/devices\/me\/\$\{deviceId\}`, data\)/)
  assert.match(profileApiSource, /apiClient\.delete\(`\/devices\/me\/\$\{deviceId\}`\)/)
})
