const trimTrailingSlash = (value) => value.replace(/\/+$/, '')

const ensureLeadingSlash = (path) => (path.startsWith('/') ? path : `/${path}`)

const viteEnv = import.meta.env ?? {}

export const env = {
  apiBaseUrl: viteEnv.VITE_API_BASE_URL || '/api',
  wsBaseUrl: viteEnv.VITE_WS_BASE_URL || '/ws',
  frontendUrl: viteEnv.VITE_FRONTEND_URL || globalThis.location?.origin || 'http://localhost:5173',
}

export function getHttpEndpoint(path = env.wsBaseUrl) {
  if (/^https?:\/\//i.test(path)) {
    return path
  }

  const normalizedPath = ensureLeadingSlash(path)
  return `${window.location.origin}${normalizedPath}`
}

export function getWebSocketEndpoint(path = env.wsBaseUrl) {
  if (/^wss?:\/\//i.test(path)) {
    return path
  }

  if (/^https?:\/\//i.test(path)) {
    return path.replace(/^http/i, 'ws')
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}${ensureLeadingSlash(path)}`
}

export function appendQuery(url, params) {
  const target = new URL(url)
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      target.searchParams.set(key, value)
    }
  })
  return target.toString()
}

export function joinUrl(baseUrl, path) {
  if (!baseUrl) {
    return ensureLeadingSlash(path)
  }

  return `${trimTrailingSlash(baseUrl)}${ensureLeadingSlash(path)}`
}
