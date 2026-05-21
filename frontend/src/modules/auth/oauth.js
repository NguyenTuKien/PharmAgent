import { env, joinUrl } from '../../config/env.js'

export function buildGoogleOAuthStartUrl(apiBaseUrl = env.apiBaseUrl) {
  return joinUrl(apiBaseUrl, '/auth/oauth/google/start')
}

export function readGoogleOAuthCallback(search = globalThis.location?.search ?? '') {
  const params = new URLSearchParams(search)
  if (params.get('oauth') !== 'google') {
    return null
  }

  return {
    provider: 'google',
    code: params.get('code') || '',
    error: params.get('error') || '',
  }
}

export function redirectToGoogleOAuth({
  apiBaseUrl = env.apiBaseUrl,
  location = globalThis.location,
} = {}) {
  location.assign(buildGoogleOAuthStartUrl(apiBaseUrl))
}
