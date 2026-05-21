import { apiClient } from '../../lib/apiClient.js'

export async function loginRequest(credentials) {
  const response = await apiClient.post('/auth/login', credentials, {
    skipAuthHeader: true,
    skipAuthRefresh: true,
  })
  return response.data
}

export async function registerRequest(payload) {
  const response = await apiClient.post('/auth/register', payload, {
    skipAuthHeader: true,
    skipAuthRefresh: true,
  })
  return response.data
}

export async function registerElderlyRequest(onboardingToken, payload) {
  const response = await apiClient.post('/auth/register/elderly', payload, {
    headers: {
      Authorization: `Bearer ${onboardingToken}`,
    },
    skipAuthHeader: true,
    skipAuthRefresh: true,
  })
  return response.data
}

export async function verifyEmailRequest(payload) {
  const response = await apiClient.post('/auth/verify-email', payload, {
    skipAuthHeader: true,
    skipAuthRefresh: true,
  })
  return response.data
}

export async function resendVerificationRequest(email) {
  const response = await apiClient.post(
    '/auth/resend-verification',
    { email },
    {
      skipAuthHeader: true,
      skipAuthRefresh: true,
    },
  )
  return response.data
}

export async function resendOTP(email) {
  return resendVerificationRequest(email)
}

export async function forgotPasswordRequest(email) {
  const response = await apiClient.post(
    '/auth/forgot-password',
    { email },
    {
      skipAuthHeader: true,
      skipAuthRefresh: true,
    },
  )
  return response.data
}

export async function forgotPassword(email) {
  return forgotPasswordRequest(email)
}

export async function resetPasswordRequest(payload) {
  const response = await apiClient.post('/auth/reset-password', payload, {
    skipAuthHeader: true,
    skipAuthRefresh: true,
  })
  return response.data
}

export async function resetPassword(email, token, newPassword, confirmPassword = newPassword) {
  return resetPasswordRequest({
    email,
    token,
    newPassword,
    confirmPassword,
  })
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

export async function completeGoogleOAuthRequest(code) {
  const response = await apiClient.post(
    '/auth/oauth/google/session',
    { code },
    {
      skipAuthHeader: true,
      skipAuthRefresh: true,
    },
  )
  return response.data
}
