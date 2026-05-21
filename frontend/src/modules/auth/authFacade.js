import { useMemo } from 'react'

import { useAuthStore } from './authStore.js'
import { redirectToGoogleOAuth } from './oauth.js'
import { buildRegisterPayload } from './registerPayload.js'
import { getAutoSelectableProfile, getProfileLandingPath } from './session.js'

export const AUTH_ONBOARDING_STORAGE_KEY = 'pharmagent.onboarding.v1'

export function readOnboardingState(storage = globalThis.sessionStorage) {
  try {
    const raw = storage?.getItem(AUTH_ONBOARDING_STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

export function saveOnboardingState(state, storage = globalThis.sessionStorage) {
  const snapshot = {
    email: state?.email,
    onboardingToken: state?.onboardingToken,
  }

  if (!snapshot.email || !snapshot.onboardingToken) {
    storage?.removeItem(AUTH_ONBOARDING_STORAGE_KEY)
    return {}
  }

  storage?.setItem(AUTH_ONBOARDING_STORAGE_KEY, JSON.stringify(snapshot))
  return snapshot
}

export function clearOnboardingState(storage = globalThis.sessionStorage) {
  storage?.removeItem(AUTH_ONBOARDING_STORAGE_KEY)
}

export async function finalizeLoginProfiles(profiles, selectProfile) {
  const autoProfile = getAutoSelectableProfile(profiles)

  if (!autoProfile) {
    return {
      profiles,
      activeProfile: null,
      requiresProfileSelection: true,
      redirectTo: '/profiles',
    }
  }

  const activeProfile = await selectProfile(autoProfile.id)

  return {
    profiles,
    activeProfile,
    requiresProfileSelection: false,
    redirectTo: getProfileLandingPath(activeProfile ?? autoProfile),
  }
}

export function useAuth() {
  const status = useAuthStore((state) => state.status)
  const loginWithStore = useAuthStore((state) => state.login)
  const loginWithGoogleHandoffCode = useAuthStore((state) => state.loginWithGoogleHandoffCode)
  const selectProfile = useAuthStore((state) => state.selectProfile)
  const registerAccount = useAuthStore((state) => state.registerAccount)
  const verifyEmail = useAuthStore((state) => state.verifyEmail)
  const registerElderlyProfile = useAuthStore((state) => state.registerElderlyProfile)
  const logout = useAuthStore((state) => state.logout)

  return useMemo(
    () => ({
      loading: status === 'authenticating',
      login: async (email, password) => {
        const profiles = await loginWithStore({ email, password })
        return finalizeLoginProfiles(profiles, selectProfile)
      },
      register: async (username, email, password) => {
        const response = await registerAccount(
          buildRegisterPayload({
            username,
            email,
            password,
            confirmPassword: password,
          }),
        )

        saveOnboardingState({
          email: response.email || email,
          onboardingToken: response.onboardingToken,
        })

        return response
      },
      registerElderly: async (payload) => {
        const onboarding = readOnboardingState()
        if (!onboarding.onboardingToken) {
          throw new Error('Không tìm thấy phiên đăng ký. Vui lòng đăng ký lại.')
        }
        const response = await registerElderlyProfile(onboarding.onboardingToken, payload)
        saveOnboardingState({
          email: response.email || onboarding.email,
          onboardingToken: response.onboardingToken || onboarding.onboardingToken,
        })
        return response
      },
      verifyAndLogin: async (email, otp) => {
        const response = await verifyEmail({ email, otp })
        clearOnboardingState()
        return response
      },
      googleLogin: async () => {
        redirectToGoogleOAuth()
      },
      completeGoogleLogin: async (code) => {
        const profiles = await loginWithGoogleHandoffCode(code)
        return finalizeLoginProfiles(profiles, selectProfile)
      },
      logout,
    }),
    [
      loginWithGoogleHandoffCode,
      loginWithStore,
      logout,
      registerAccount,
      registerElderlyProfile,
      selectProfile,
      status,
      verifyEmail,
    ],
  )
}
