import { apiClient } from '../../lib/apiClient.js'

// ─── Users ────────────────────────────────────────────────────────────────────

export async function getAllUsers({ page = 0, size = 10 } = {}) {
  const response = await apiClient.get('/admin/users', { params: { page, size } })
  return response.data
}

export async function createUser(data) {
  const response = await apiClient.post('/admin/users', data)
  return response.data
}

export async function updateUser(id, data) {
  const response = await apiClient.put(`/admin/users/${id}`, data)
  return response.data
}

export async function deleteUser(id) {
  await apiClient.delete(`/admin/users/${id}`)
}

export async function lockUser(id) {
  const response = await apiClient.patch(`/admin/users/${id}/lock`)
  return response.data
}

export async function unlockUser(id) {
  const response = await apiClient.patch(`/admin/users/${id}/unlock`)
  return response.data
}

// ─── Pills ────────────────────────────────────────────────────────────────────

export async function getPillCatalog({ search = '', page = 0, size = 10 } = {}) {
  const response = await apiClient.get('/pills', { params: { search: search || undefined, page, size } })
  return response.data
}

export async function createPill(data) {
  const response = await apiClient.post('/admin/pills', data)
  return response.data
}

export async function updatePill(id, data) {
  const response = await apiClient.put(`/admin/pills/${id}`, data)
  return response.data
}

export async function deletePill(id) {
  await apiClient.delete(`/admin/pills/${id}`)
}

export async function addPillImage(pillId, data) {
  const response = await apiClient.post(`/admin/pills/${pillId}/images`, data)
  return response.data
}

export async function deletePillImage(pillId, imageId) {
  await apiClient.delete(`/admin/pills/${pillId}/images/${imageId}`)
}
