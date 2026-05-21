import { apiClient } from '../../lib/apiClient.js'

export async function getMyProfile() {
  const response = await apiClient.get('/profiles/me')
  return response.data
}

export async function updateMyProfile(data) {
  const response = await apiClient.put('/profiles/me', data)
  return response.data
}

export async function updateMyAvatar(avatarUrl) {
  const response = await apiClient.patch('/profiles/me/avatar', { avatarUrl })
  return response.data
}

export async function getMyContacts() {
  const response = await apiClient.get('/profiles/me/contacts')
  return response.data
}

export async function addMyContact(data) {
  const response = await apiClient.post('/profiles/me/contacts', data)
  return response.data
}

export async function updateMyContact(contactId, data) {
  const response = await apiClient.put(`/profiles/me/contacts/${contactId}`, data)
  return response.data
}

export async function deleteMyContact(contactId) {
  const response = await apiClient.delete(`/profiles/me/contacts/${contactId}`)
  return response.data
}

export async function getProfiles({ page = 0, size = 20 } = {}) {
  const response = await apiClient.get('/profiles', { params: { page, size } })
  return response.data
}

export async function createManagedElderlyProfile(data) {
  const response = await apiClient.post('/caregiver/profiles', {
    ...data,
    role: 'ELDERLY',
  })
  return response.data
}

export async function getManagedElderlyProfile(profileId) {
  const response = await apiClient.get(`/caregiver/profiles/${profileId}`)
  return response.data
}

export async function updateManagedElderlyProfile(profileId, data) {
  const response = await apiClient.put(`/caregiver/profiles/${profileId}`, data)
  return response.data
}

export async function deleteManagedElderlyProfile(profileId) {
  await apiClient.delete(`/caregiver/profiles/${profileId}`)
}

export async function searchElderlyProfiles({ query = '', page = 0, size = 8 } = {}) {
  const response = await apiClient.post('/caregiver/profiles/search', undefined, {
    params: {
      query,
      role: 'ELDERLY',
      page,
      size,
    },
  })
  return response.data
}

export async function getCaregiverRelationships() {
  const response = await apiClient.get('/caregiver/relationship')
  return response.data
}

export async function getPendingCaregiverRelationships() {
  const response = await apiClient.get('/caregiver/relationship/pending')
  return response.data
}

export async function inviteElderlyProfile(data) {
  const response = await apiClient.post('/caregiver/relationship/invite', data)
  return response.data
}

export async function updateCaregiverRelationship(targetElderlyId, permissionLevel) {
  const response = await apiClient.patch(`/caregiver/relationship/${targetElderlyId}`, undefined, {
    params: { permissionLevel },
  })
  return response.data
}

export async function getElderlyRelationships() {
  const response = await apiClient.get('/elderly/relationship')
  return response.data
}

export async function getPendingElderlyRelationships() {
  const response = await apiClient.get('/elderly/relationship/pending')
  return response.data
}

export async function acceptCaregiverInvitation(relationshipId) {
  const response = await apiClient.put(`/elderly/relationship/${relationshipId}/accept`)
  return response.data
}

export async function refuseCaregiverInvitation(relationshipId) {
  const response = await apiClient.put(`/elderly/relationship/${relationshipId}/refuse`)
  return response.data
}

export async function changePassword(data) {
  const response = await apiClient.post('/auth/change-password', data)
  return response.data
}
