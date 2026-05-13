import axios from 'axios'

import { env } from '../config/env.js'

const authHandlers = {
  getAccessToken: undefined,
  refreshSession: undefined,
  clearSession: undefined,
}

export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

export function setApiAuthHandlers(handlers) {
  Object.assign(authHandlers, handlers)
}

export function getApiErrorMessage(error) {
  if (error?.response?.data?.detail) {
    return error.response.data.detail
  }

  if (error?.response?.data?.message) {
    return error.response.data.message
  }

  if (error?.response?.data?.error) {
    return error.response.data.error
  }

  if (error?.message) {
    return error.message
  }

  return 'Request failed'
}

apiClient.interceptors.request.use((config) => {
  if (!config.skipAuthHeader) {
    const accessToken = authHandlers.getAccessToken?.()
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const shouldRefresh =
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest.skipAuthRefresh

    if (shouldRefresh) {
      originalRequest._retry = true

      try {
        const accessToken = await authHandlers.refreshSession?.()
        if (accessToken) {
          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return apiClient(originalRequest)
        }
      } catch {
        authHandlers.clearSession?.()
      }
    }

    return Promise.reject(error)
  },
)
