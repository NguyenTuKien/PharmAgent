import { apiClient } from '../../lib/apiClient.js'

export async function loginRequest(credentials) {
  const response = await apiClient.post('/auth/login', credentials, {
    skipAuthHeader: true,
    skipAuthRefresh: true,
  })
  return response.data
}

export async function selectProfileRequest(authToken, profileId) {
  const response = await apiClient.post(`/auth/profiles/${profileId}/select`, undefined, {
    headers: {
      Authorization: `Bearer ${authToken}`,
    },
    skipAuthHeader: true,
    skipAuthRefresh: true,
  })
  return response.data
}

export async function refreshTokensRequest(refreshToken, profileId) {
  const response = await apiClient.post(
    '/auth/refresh',
    { refreshToken, profileId },
    {
      skipAuthHeader: true,
      skipAuthRefresh: true,
    },
  )
  return response.data
}

export async function logoutRequest({ authToken, accessToken, refreshToken }) {
  await apiClient.post(
    '/auth/logout',
    { authToken, accessToken, refreshToken },
    {
      skipAuthHeader: true,
      skipAuthRefresh: true,
    },
  )
}
