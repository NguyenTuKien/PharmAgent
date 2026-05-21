import { create } from 'zustand'

import { setApiAuthHandlers } from '../../lib/apiClient.js'
import {
  completeGoogleOAuthRequest,
  forgotPasswordRequest,
  loginRequest,
  logoutRequest,
  registerElderlyRequest,
  registerRequest,
  resendVerificationRequest,
  resetPasswordRequest,
  refreshTokensRequest,
  selectProfileRequest,
  verifyEmailRequest,
} from './authApi.js'
import {
  buildSessionSnapshot,
  clearSession,
  loadSession,
  normalizeProfilesPage,
  saveSession,
} from './session.js'

const persistedSession = loadSession()
const persistedProfile = persistedSession.activeProfileId
  ? {
      id: persistedSession.activeProfileId,
      role: persistedSession.activeRole,
    }
  : null

const initialAuthState = {
  authToken: persistedSession.authToken ?? null,
  refreshToken: persistedSession.refreshToken ?? null,
  accessToken: persistedSession.accessToken ?? null,
  profiles: [],
  activeProfile: persistedProfile,
  status: persistedSession.refreshToken ? 'authenticated' : 'anonymous',
  error: null,
}

function applyLoginResponse(response, set) {
  const profiles = normalizeProfilesPage(response.profiles)

  saveSession({
    authToken: response.authToken,
    refreshToken: response.refreshToken,
  })

  set({
    authToken: response.authToken,
    refreshToken: response.refreshToken,
    accessToken: null,
    profiles,
    activeProfile: null,
    status: 'profile_required',
    error: null,
  })

  return profiles
}

export const useAuthStore = create((set, get) => ({
  ...initialAuthState,

  login: async (credentials) => {
    set({ status: 'authenticating', error: null })
    const response = await loginRequest(credentials)
    return applyLoginResponse(response, set)
  },

  loginWithGoogleHandoffCode: async (code) => {
    set({ status: 'authenticating', error: null })
    try {
      const response = await completeGoogleOAuthRequest(code)
      return applyLoginResponse(response, set)
    } catch (error) {
      clearSession()
      set({
        authToken: null,
        refreshToken: null,
        accessToken: null,
        profiles: [],
        activeProfile: null,
        status: 'anonymous',
        error,
      })
      throw error
    }
  },

  registerAccount: async (payload) => {
    set({ status: 'authenticating', error: null })
    try {
      const response = await registerRequest(payload)
      set({ status: 'verification_required', error: null })
      return response
    } catch (error) {
      set({ status: 'anonymous', error })
      throw error
    }
  },

  registerElderlyProfile: async (onboardingToken, payload) => {
    const response = await registerElderlyRequest(onboardingToken, payload)
    set({ status: 'verification_required', error: null })
    return response
  },

  verifyEmail: async (payload) => {
    const response = await verifyEmailRequest(payload)
    set({ status: 'anonymous', error: null })
    return response
  },

  resendVerification: async (email) => resendVerificationRequest(email),

  requestPasswordReset: async (email) => forgotPasswordRequest(email),

  resetPassword: async (payload) => resetPasswordRequest(payload),

  selectProfile: async (profileId) => {
    const { authToken, refreshToken, profiles } = get()
    const profile = profiles.find((item) => item.id === profileId) ?? { id: profileId }
    const response = await selectProfileRequest(authToken, profileId)
    const activeProfile = {
      ...profile,
      id: profileId,
    }

    const snapshot = buildSessionSnapshot({
      authToken,
      refreshToken,
      accessToken: response.accessToken,
      activeProfile,
    })
    saveSession(snapshot)

    set({
      accessToken: response.accessToken,
      activeProfile,
      status: 'authenticated',
      error: null,
    })

    return activeProfile
  },

  refreshSession: async () => {
    const { refreshToken, activeProfile, authToken } = get()
    if (!refreshToken || !activeProfile?.id) {
      get().clearLocalSession()
      throw new Error('Missing refresh token or active profile')
    }

    const response = await refreshTokensRequest(refreshToken, activeProfile.id)
    const nextAuthToken = response.authToken ?? authToken
    const nextRefreshToken = response.refreshToken ?? refreshToken
    const nextAccessToken = response.accessToken

    const snapshot = buildSessionSnapshot({
      authToken: nextAuthToken,
      refreshToken: nextRefreshToken,
      accessToken: nextAccessToken,
      activeProfile,
    })
    saveSession(snapshot)

    set({
      authToken: nextAuthToken,
      refreshToken: nextRefreshToken,
      accessToken: nextAccessToken,
      status: 'authenticated',
      error: null,
    })

    return nextAccessToken
  },

  logout: async () => {
    const { authToken, accessToken, refreshToken } = get()

    try {
      if (refreshToken) {
        await logoutRequest({ authToken, accessToken, refreshToken })
      }
    } finally {
      get().clearLocalSession()
    }
  },

  clearLocalSession: () => {
    clearSession()
    set({
      authToken: null,
      refreshToken: null,
      accessToken: null,
      profiles: [],
      activeProfile: null,
      status: 'anonymous',
      error: null,
    })
  },
}))

setApiAuthHandlers({
  getAccessToken: () => useAuthStore.getState().accessToken,
  refreshSession: () => useAuthStore.getState().refreshSession(),
  clearSession: () => useAuthStore.getState().clearLocalSession(),
})
