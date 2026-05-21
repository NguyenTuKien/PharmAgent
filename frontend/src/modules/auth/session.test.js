import assert from 'node:assert/strict'
import { test } from 'node:test'

import {
  buildSessionSnapshot,
  canAccessRoles,
  clearSession,
  loadSession,
  normalizeProfilesPage,
  saveSession,
  sanitizeSession,
} from './session.js'

function createMemoryStorage() {
  const values = new Map()

  return {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: (key) => values.delete(key),
  }
}

test('normalizeProfilesPage returns Spring Page content when provided', () => {
  const profiles = normalizeProfilesPage({
    content: [
      { id: 'profile-1', firstName: 'An', role: 'CAREGIVER' },
      { id: 'profile-2', firstName: 'Binh', role: 'ELDERLY' },
    ],
    totalElements: 2,
  })

  assert.deepEqual(profiles, [
    { id: 'profile-1', firstName: 'An', role: 'CAREGIVER' },
    { id: 'profile-2', firstName: 'Binh', role: 'ELDERLY' },
  ])
})

test('sanitizeSession strips unknown fields and keeps only usable session values', () => {
  const sanitized = sanitizeSession({
    authToken: 'auth-token',
    refreshToken: 'refresh-token',
    accessToken: 'access-token',
    activeProfileId: 'profile-1',
    activeRole: 'CAREGIVER',
    unsafe: 'remove-me',
  })

  assert.deepEqual(sanitized, {
    authToken: 'auth-token',
    refreshToken: 'refresh-token',
    accessToken: 'access-token',
    activeProfileId: 'profile-1',
    activeRole: 'CAREGIVER',
  })
})

test('buildSessionSnapshot keeps selected profile metadata with tokens', () => {
  const snapshot = buildSessionSnapshot({
    authToken: 'auth-token',
    refreshToken: 'refresh-token',
    accessToken: 'access-token',
    activeProfile: { id: 'profile-1', role: 'ELDERLY' },
  })

  assert.deepEqual(snapshot, {
    authToken: 'auth-token',
    refreshToken: 'refresh-token',
    accessToken: 'access-token',
    activeProfileId: 'profile-1',
    activeRole: 'ELDERLY',
  })
})

test('canAccessRoles accepts roles case-insensitively and allows empty requirements', () => {
  assert.equal(canAccessRoles('caregiver', ['CAREGIVER']), true)
  assert.equal(canAccessRoles('ELDERLY', ['caregiver']), false)
  assert.equal(canAccessRoles('ADMIN', []), true)
  assert.equal(canAccessRoles(undefined, ['ADMIN']), false)
})

test('session helpers use localStorage by default so auth survives new tabs', () => {
  const originalLocalStorage = globalThis.localStorage
  const originalSessionStorage = globalThis.sessionStorage
  const localStorage = createMemoryStorage()
  const sessionStorage = createMemoryStorage()

  globalThis.localStorage = localStorage
  globalThis.sessionStorage = sessionStorage

  try {
    const session = {
      authToken: 'auth-token',
      refreshToken: 'refresh-token',
      accessToken: 'access-token',
      activeProfileId: 'profile-1',
      activeRole: 'CAREGIVER',
    }

    saveSession(session)

    assert.deepEqual(loadSession(), session)
    assert.equal(sessionStorage.getItem('pharmagent.session.v1'), null)

    clearSession()

    assert.deepEqual(loadSession(), {})
  } finally {
    globalThis.localStorage = originalLocalStorage
    globalThis.sessionStorage = originalSessionStorage
  }
})
