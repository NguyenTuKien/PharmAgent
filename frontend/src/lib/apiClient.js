import axios from 'axios'

import { env } from '../config/env.js'

const authHandlers = {
  getAccessToken: undefined,
  refreshSession: undefined,
  clearSession: undefined,
}

let refreshSessionRequest = null

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

function refreshAccessToken() {
  if (!refreshSessionRequest) {
    refreshSessionRequest = Promise.resolve()
      .then(() => authHandlers.refreshSession?.())
      .finally(() => {
        refreshSessionRequest = null
      })
  }

  return refreshSessionRequest
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

  if (Array.isArray(error?.response?.data?.errors)) {
    return error.response.data.errors.map((e) => e.defaultMessage || e.msg || e).join(', ')
  }

  if (error?.message) {
    return error.message
  }

  return 'Request failed'
}

function isFormDataPayload(data) {
  return typeof FormData !== 'undefined' && data instanceof FormData
}

function deleteHeader(headers, headerName) {
  if (!headers) {
    return
  }

  if (typeof headers.delete === 'function') {
    headers.delete(headerName)
    return
  }

  Object.keys(headers).forEach((key) => {
    if (key.toLowerCase() === headerName.toLowerCase()) {
      delete headers[key]
    }
  })
}

apiClient.interceptors.request.use((config) => {
  if (isFormDataPayload(config.data)) {
    deleteHeader(config.headers, 'Content-Type')
  }

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
        const accessToken = await refreshAccessToken()
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
