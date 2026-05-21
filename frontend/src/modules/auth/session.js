export const SESSION_STORAGE_KEY = 'pharmagent.session.v1'

const SESSION_FIELDS = [
  'authToken',
  'refreshToken',
  'accessToken',
  'activeProfileId',
  'activeRole',
]

const PROFILE_FIELDS = [
  'id',
  'userId',
  'phone',
  'firstName',
  'lastName',
  'avatarUrl',
  'role',
]

const normalizeString = (value) => (typeof value === 'string' && value.trim() ? value : null)
const normalizeRole = (role) => normalizeString(role)?.toUpperCase() ?? null

function sanitizeProfile(profile) {
  if (!profile || typeof profile !== 'object') {
    return null
  }

  const normalized = PROFILE_FIELDS.reduce((snapshot, key) => {
    const value = normalizeString(profile[key])
    if (value) {
      snapshot[key] = value
    }
    return snapshot
  }, {})

  return normalized.id ? normalized : null
}

export function normalizeProfilesPage(page) {
  let profiles = []

  if (Array.isArray(page)) {
    profiles = page
  } else if (Array.isArray(page?.content)) {
    profiles = page.content
  }

  return profiles.map(sanitizeProfile).filter(Boolean)
}

export function sanitizeSession(session) {
  if (!session || typeof session !== 'object') {
    return {}
  }

  const snapshot = SESSION_FIELDS.reduce((nextSnapshot, key) => {
    const value = normalizeString(session[key])
    if (value) {
      nextSnapshot[key] = value
    }
    return nextSnapshot
  }, {})

  const profiles = normalizeProfilesPage(session.profiles)
  if (profiles.length) {
    snapshot.profiles = profiles
  }

  const legacyProfile = sanitizeProfile({
    id: snapshot.activeProfileId,
    role: snapshot.activeRole,
  })
  const explicitProfile = sanitizeProfile({
    ...legacyProfile,
    ...session.activeProfile,
  })
  const activeProfile = explicitProfile ?? legacyProfile

  if (activeProfile) {
    snapshot.activeProfile = activeProfile
    snapshot.activeProfileId = activeProfile.id
    if (activeProfile.role) {
      snapshot.activeRole = activeProfile.role
    }
  }

  return snapshot
}

export function buildSessionSnapshot({ authToken, refreshToken, accessToken, activeProfile, profiles }) {
  return sanitizeSession({
    authToken,
    refreshToken,
    accessToken,
    profiles,
    activeProfile,
    activeProfileId: activeProfile?.id,
    activeRole: activeProfile?.role,
  })
}

export function deriveAuthStateFromSession(session) {
  const snapshot = sanitizeSession(session)
  const activeProfile = snapshot.activeProfile ?? null
  const profiles = snapshot.profiles ?? []
  const authToken = snapshot.authToken ?? null
  const refreshToken = snapshot.refreshToken ?? null
  const accessToken = snapshot.accessToken ?? null

  let status = 'anonymous'
  if (activeProfile?.id && accessToken) {
    status = 'authenticated'
  } else if (activeProfile?.id && refreshToken) {
    status = 'restoring'
  } else if (authToken || refreshToken) {
    status = 'profile_required'
  }

  return {
    authToken,
    refreshToken,
    accessToken,
    profiles,
    activeProfile,
    status,
    error: null,
  }
}

export function canAccessRoles(activeRole, requiredRoles = []) {
  if (!requiredRoles.length) {
    return true
  }

  const normalizedRole = normalizeRole(activeRole)
  if (!normalizedRole) {
    return false
  }

  return requiredRoles.some((role) => normalizeRole(role) === normalizedRole)
}

export function getAutoSelectableProfile(profiles = []) {
  const normalizedProfiles = normalizeProfilesPage(profiles)
  if (!normalizedProfiles.length) {
    return null
  }

  const adminProfile = normalizedProfiles.find((profile) => normalizeRole(profile.role) === 'ADMIN')
  if (adminProfile) {
    return adminProfile
  }

  const hasLinkedElderly = normalizedProfiles.some(
    (profile) => normalizeRole(profile.role) === 'ELDERLY',
  )
  const caregiverProfiles = normalizedProfiles.filter(
    (profile) => normalizeRole(profile.role) === 'CAREGIVER',
  )

  if (!hasLinkedElderly && caregiverProfiles.length === 1) {
    return caregiverProfiles[0]
  }

  if (normalizedProfiles.length === 1 && !hasLinkedElderly) {
    return normalizedProfiles[0]
  }

  return null
}

export function requiresProfileSelection(profiles = []) {
  return normalizeProfilesPage(profiles).length > 0 && !getAutoSelectableProfile(profiles)
}

export function getProfileLandingPath(profile) {
  return normalizeRole(profile?.role) === 'ADMIN' ? '/admin' : '/dashboard'
}

function getDefaultSessionStorage() {
  return globalThis.localStorage ?? globalThis.sessionStorage
}

export function loadSession(storage = getDefaultSessionStorage()) {
  try {
    const raw = storage?.getItem(SESSION_STORAGE_KEY)
    return raw ? sanitizeSession(JSON.parse(raw)) : {}
  } catch {
    return {}
  }
}

export function saveSession(session, storage = getDefaultSessionStorage()) {
  const snapshot = sanitizeSession(session)

  if (!Object.keys(snapshot).length) {
    storage?.removeItem(SESSION_STORAGE_KEY)
    return snapshot
  }

  storage?.setItem(SESSION_STORAGE_KEY, JSON.stringify(snapshot))
  return snapshot
}

export function clearSession(storage = getDefaultSessionStorage()) {
  storage?.removeItem(SESSION_STORAGE_KEY)
}
