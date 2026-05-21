import { apiClient } from '../../lib/apiClient.js'

export function asPageContent(pageOrList) {
  if (Array.isArray(pageOrList)) {
    return pageOrList
  }
  return Array.isArray(pageOrList?.content) ? pageOrList.content : []
}

export function normalizePillId(rawId) {
  if (!rawId) {
    return ''
  }
  return String(rawId).replace(/^local:\/\/pill\//, '')
}

export async function searchPills(keyword, { limit = 10 } = {}) {
  const response = await apiClient.get('/pills/search', {
    params: {
      keyword,
      limit,
    },
  })
  return asPageContent(response.data).map((pill) => ({
    ...pill,
    id: normalizePillId(pill.id),
  }))
}

export async function getPillById(pillId) {
  const response = await apiClient.get(`/pills/${normalizePillId(pillId)}`)
  return {
    ...response.data,
    id: normalizePillId(response.data?.id ?? pillId),
  }
}

export async function analyzeMedicationImage(file) {
  const formData = new FormData()
  formData.append('image', file)
  formData.append('top_k', '6')

  const response = await apiClient.post('/agent/analyze', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}

export async function getMedications({ patientId, isActive = true, page = 0, size = 100 }) {
  const response = await apiClient.get('/medications', {
    params: {
      patientId,
      isActive,
      page,
      size,
    },
  })
  return response.data
}

export async function getMedicationById(medicationId) {
  const response = await apiClient.get(`/medications/${medicationId}`)
  return response.data
}

export async function createCaregiverMedication(data) {
  const response = await apiClient.post('/caregiver/medications', data)
  return response.data
}

export async function updateCaregiverMedication(medicationId, data) {
  const response = await apiClient.put(`/caregiver/medications/${medicationId}`, data)
  return response.data
}

export async function deleteCaregiverMedication(medicationId) {
  await apiClient.delete(`/caregiver/medications/${medicationId}`)
}

export async function addMedicationSchedule(medicationId, schedule) {
  const response = await apiClient.post(`/caregiver/medications/${medicationId}/schedules`, schedule)
  return response.data
}

export async function updateMedicationSchedule(medicationId, scheduleId, schedule) {
  const response = await apiClient.put(
    `/caregiver/medications/${medicationId}/schedules/${scheduleId}`,
    schedule,
  )
  return response.data
}

export async function deleteMedicationSchedule(medicationId, scheduleId) {
  const response = await apiClient.delete(`/caregiver/medications/${medicationId}/schedules/${scheduleId}`)
  return response.data
}

export async function addScheduleTime(medicationId, scheduleId, time) {
  const response = await apiClient.post(
    `/caregiver/medications/${medicationId}/schedules/${scheduleId}/times`,
    time,
  )
  return response.data
}

export async function updateScheduleTime(medicationId, scheduleId, timeId, time) {
  const response = await apiClient.put(
    `/caregiver/medications/${medicationId}/schedules/${scheduleId}/times/${timeId}`,
    time,
  )
  return response.data
}

export async function deleteScheduleTime(medicationId, scheduleId, timeId) {
  const response = await apiClient.delete(
    `/caregiver/medications/${medicationId}/schedules/${scheduleId}/times/${timeId}`,
  )
  return response.data
}
