import assert from 'node:assert/strict'
import { test } from 'node:test'

import {
  buildSessionSnapshot,
  canAccessRoles,
  clearSession,
  deriveAuthStateFromSession,
  getAutoSelectableProfile,
  getProfileLandingPath,
  loadSession,
  normalizeProfilesPage,
  requiresProfileSelection,
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
    profiles: [
      {
        id: 'profile-1',
        userId: 'user-1',
        firstName: 'An',
        lastName: 'Nguyen',
        avatarUrl: 'https://example.test/avatar.png',
        role: 'CAREGIVER',
        unsafe: 'remove-me',
      },
      { id: '', firstName: 'Invalid' },
    ],
    activeProfile: {
      id: 'profile-1',
      userId: 'user-1',
      firstName: 'An',
      lastName: 'Nguyen',
      avatarUrl: 'https://example.test/avatar.png',
      role: 'CAREGIVER',
      unsafe: 'remove-me',
    },
    unsafe: 'remove-me',
  })

  assert.deepEqual(sanitized, {
    authToken: 'auth-token',
    refreshToken: 'refresh-token',
    accessToken: 'access-token',
    activeProfileId: 'profile-1',
    activeRole: 'CAREGIVER',
    profiles: [
      {
        id: 'profile-1',
        userId: 'user-1',
        firstName: 'An',
        lastName: 'Nguyen',
        avatarUrl: 'https://example.test/avatar.png',
        role: 'CAREGIVER',
      },
    ],
    activeProfile: {
      id: 'profile-1',
      userId: 'user-1',
      firstName: 'An',
      lastName: 'Nguyen',
      avatarUrl: 'https://example.test/avatar.png',
      role: 'CAREGIVER',
    },
  })
})

test('buildSessionSnapshot keeps selected profile metadata with tokens', () => {
  const snapshot = buildSessionSnapshot({
    authToken: 'auth-token',
    refreshToken: 'refresh-token',
    accessToken: 'access-token',
    profiles: [
      {
        id: 'profile-1',
        firstName: 'Binh',
        lastName: 'Tran',
        role: 'ELDERLY',
      },
    ],
    activeProfile: {
      id: 'profile-1',
      firstName: 'Binh',
      lastName: 'Tran',
      role: 'ELDERLY',
    },
  })

  assert.deepEqual(snapshot, {
    authToken: 'auth-token',
    refreshToken: 'refresh-token',
    accessToken: 'access-token',
    activeProfileId: 'profile-1',
    activeRole: 'ELDERLY',
    profiles: [
      {
        id: 'profile-1',
        firstName: 'Binh',
        lastName: 'Tran',
        role: 'ELDERLY',
      },
    ],
    activeProfile: {
      id: 'profile-1',
      firstName: 'Binh',
      lastName: 'Tran',
      role: 'ELDERLY',
    },
  })
})

test('deriveAuthStateFromSession restores full authenticated profile from storage', () => {
  const state = deriveAuthStateFromSession({
    authToken: 'auth-token',
    refreshToken: 'refresh-token',
    accessToken: 'access-token',
    activeProfile: {
      id: 'profile-1',
      firstName: 'Binh',
      lastName: 'Tran',
      role: 'ELDERLY',
    },
    profiles: [
      {
        id: 'profile-1',
        firstName: 'Binh',
        lastName: 'Tran',
        role: 'ELDERLY',
      },
    ],
  })

  assert.equal(state.status, 'authenticated')
  assert.deepEqual(state.activeProfile, {
    id: 'profile-1',
    firstName: 'Binh',
    lastName: 'Tran',
    role: 'ELDERLY',
  })
  assert.deepEqual(state.profiles, [
    {
      id: 'profile-1',
      firstName: 'Binh',
      lastName: 'Tran',
      role: 'ELDERLY',
    },
  ])
})

test('deriveAuthStateFromSession waits for refresh before redirecting protected pages', () => {
  const state = deriveAuthStateFromSession({
    refreshToken: 'refresh-token',
    activeProfile: {
      id: 'profile-1',
      role: 'CAREGIVER',
    },
  })

  assert.equal(state.status, 'restoring')
  assert.equal(state.accessToken, null)
  assert.deepEqual(state.activeProfile, {
    id: 'profile-1',
    role: 'CAREGIVER',
  })
})

test('canAccessRoles accepts roles case-insensitively and allows empty requirements', () => {
  assert.equal(canAccessRoles('caregiver', ['CAREGIVER']), true)
  assert.equal(canAccessRoles('ELDERLY', ['caregiver']), false)
  assert.equal(canAccessRoles('ADMIN', []), true)
  assert.equal(canAccessRoles(undefined, ['ADMIN']), false)
})

test('getAutoSelectableProfile skips profile selection for admin accounts', () => {
  const adminProfile = {
    id: 'admin-profile',
    firstName: 'Minh',
    role: 'ADMIN',
  }

  assert.deepEqual(getAutoSelectableProfile([adminProfile]), adminProfile)
  assert.equal(requiresProfileSelection([adminProfile]), false)
})

test('getAutoSelectableProfile skips profile selection for caregivers without elderly profiles', () => {
  const caregiverProfile = {
    id: 'caregiver-profile',
    firstName: 'An',
    role: 'CAREGIVER',
  }

  assert.deepEqual(getAutoSelectableProfile([caregiverProfile]), caregiverProfile)
  assert.equal(requiresProfileSelection([caregiverProfile]), false)
})

test('requiresProfileSelection asks caregiver to choose when linked elderly profiles exist', () => {
  const profiles = [
    { id: 'caregiver-profile', firstName: 'An', role: 'CAREGIVER' },
    { id: 'elderly-profile', firstName: 'Binh', role: 'ELDERLY' },
  ]

  assert.equal(getAutoSelectableProfile(profiles), null)
  assert.equal(requiresProfileSelection(profiles), true)
})

test('getProfileLandingPath sends admin profiles to admin area and others to dashboard', () => {
  assert.equal(getProfileLandingPath({ role: 'ADMIN' }), '/admin')
  assert.equal(getProfileLandingPath({ role: 'CAREGIVER' }), '/dashboard')
  assert.equal(getProfileLandingPath({ role: 'ELDERLY' }), '/dashboard')
  assert.equal(getProfileLandingPath(null), '/dashboard')
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
      activeProfile: {
        id: 'profile-1',
        firstName: 'An',
        role: 'CAREGIVER',
      },
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
