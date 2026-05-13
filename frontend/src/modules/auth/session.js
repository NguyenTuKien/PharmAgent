export const SESSION_STORAGE_KEY = 'pharmagent.session.v1'

const SESSION_FIELDS = [
  'authToken',
  'refreshToken',
  'accessToken',
  'activeProfileId',
  'activeRole',
]

const normalizeString = (value) => (typeof value === 'string' && value.trim() ? value : null)

export function normalizeProfilesPage(page) {
  if (Array.isArray(page)) {
    return page
  }

  if (Array.isArray(page?.content)) {
    return page.content
  }

  return []
}

export function sanitizeSession(session) {
  if (!session || typeof session !== 'object') {
    return {}
  }

  return SESSION_FIELDS.reduce((snapshot, key) => {
    const value = normalizeString(session[key])
    if (value) {
      snapshot[key] = value
    }
    return snapshot
  }, {})
}

export function buildSessionSnapshot({ authToken, refreshToken, accessToken, activeProfile }) {
  return sanitizeSession({
    authToken,
    refreshToken,
    accessToken,
    activeProfileId: activeProfile?.id,
    activeRole: activeProfile?.role,
  })
}

export function canAccessRoles(activeRole, requiredRoles = []) {
  if (!requiredRoles.length) {
    return true
  }

  const normalizedRole = normalizeString(activeRole)?.toUpperCase()
  if (!normalizedRole) {
    return false
  }

  return requiredRoles.some((role) => normalizeString(role)?.toUpperCase() === normalizedRole)
}

export function loadSession(storage = globalThis.sessionStorage) {
  try {
    const raw = storage?.getItem(SESSION_STORAGE_KEY)
    return raw ? sanitizeSession(JSON.parse(raw)) : {}
  } catch {
    return {}
  }
}

export function saveSession(session, storage = globalThis.sessionStorage) {
  const snapshot = sanitizeSession(session)

  if (!Object.keys(snapshot).length) {
    storage?.removeItem(SESSION_STORAGE_KEY)
    return snapshot
  }

  storage?.setItem(SESSION_STORAGE_KEY, JSON.stringify(snapshot))
  return snapshot
}

export function clearSession(storage = globalThis.sessionStorage) {
  storage?.removeItem(SESSION_STORAGE_KEY)
}
