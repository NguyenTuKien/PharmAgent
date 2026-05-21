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
  deriveAuthStateFromSession,
  loadSession,
  normalizeProfilesPage,
  saveSession,
} from './session.js'

const initialAuthState = deriveAuthStateFromSession(loadSession())

function applyLoginResponse(response, set) {
  const profiles = normalizeProfilesPage(response.profiles)

  saveSession({
    authToken: response.authToken,
    refreshToken: response.refreshToken,
    profiles,
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

function isUnauthorized(error) {
  return error?.response?.status === 401
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
    let { authToken, refreshToken, profiles } = get()
    const profile = profiles.find((item) => item.id === profileId) ?? { id: profileId }

    const refreshAuthToken = async () => {
      if (!refreshToken) {
        throw new Error('Missing refresh token')
      }

      const refreshResponse = await refreshTokensRequest(refreshToken)
      authToken = refreshResponse.authToken
      refreshToken = refreshResponse.refreshToken ?? refreshToken

      const pendingSnapshot = buildSessionSnapshot({
        authToken,
        refreshToken,
        profiles,
      })
      saveSession(pendingSnapshot)
      set({
        authToken,
        refreshToken,
        profiles: pendingSnapshot.profiles ?? profiles,
        status: 'profile_required',
        error: null,
      })
    }

    if (!authToken && refreshToken) {
      try {
        await refreshAuthToken()
      } catch (error) {
        get().clearLocalSession()
        throw error
      }
    }

    let response
    try {
      response = await selectProfileRequest(authToken, profileId)
    } catch (error) {
      if (!isUnauthorized(error) || !refreshToken) {
        throw error
      }
      try {
        await refreshAuthToken()
      } catch (refreshError) {
        get().clearLocalSession()
        throw refreshError
      }
      response = await selectProfileRequest(authToken, profileId)
    }

    const activeProfile = {
      ...profile,
      id: profileId,
    }

    const snapshot = buildSessionSnapshot({
      authToken,
      refreshToken,
      accessToken: response.accessToken,
      activeProfile,
      profiles,
    })
    saveSession(snapshot)

    set({
      accessToken: response.accessToken,
      activeProfile: snapshot.activeProfile,
      status: 'authenticated',
      error: null,
    })

    return snapshot.activeProfile
  },

  refreshSession: async () => {
    const { refreshToken, activeProfile, authToken, profiles } = get()
    if (!refreshToken || !activeProfile?.id) {
      get().clearLocalSession()
      throw new Error('Missing refresh token or active profile')
    }

    set({ status: 'restoring', error: null })

    let response
    try {
      response = await refreshTokensRequest(refreshToken, activeProfile.id)
    } catch (error) {
      get().clearLocalSession()
      throw error
    }

    const nextAuthToken = response.authToken ?? authToken
    const nextRefreshToken = response.refreshToken ?? refreshToken
    const nextAccessToken = response.accessToken

    const snapshot = buildSessionSnapshot({
      authToken: nextAuthToken,
      refreshToken: nextRefreshToken,
      accessToken: nextAccessToken,
      activeProfile,
      profiles,
    })
    saveSession(snapshot)

    set({
      authToken: nextAuthToken,
      refreshToken: nextRefreshToken,
      accessToken: nextAccessToken,
      activeProfile: snapshot.activeProfile,
      profiles: snapshot.profiles ?? profiles,
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

  syncLocalSession: () => {
    set(deriveAuthStateFromSession(loadSession()))
  },
}))

setApiAuthHandlers({
  getAccessToken: () => useAuthStore.getState().accessToken,
  refreshSession: () => useAuthStore.getState().refreshSession(),
  clearSession: () => useAuthStore.getState().clearLocalSession(),
})
