import { apiClient } from '../../lib/apiClient.js'

export function asPageContent(pageOrList) {
  if (Array.isArray(pageOrList)) {
    return pageOrList
  }

  for (const key of ['content', 'items', 'results', 'data']) {
    if (Array.isArray(pageOrList?.[key])) {
      return pageOrList[key]
    }
  }

  return []
}

export function normalizePillId(rawId) {
  if (!rawId) {
    return ''
  }
  return String(rawId).trim().replace(/^local:\/\/pill\//i, '')
}

export function normalizeCatalogPill(pill, fallbackId = '') {
  if (!pill) {
    return null
  }

  const normalizedId = normalizePillId(
    pill.id ?? pill.product_id ?? pill.source_url ?? fallbackId,
  )

  return {
    ...pill,
    id: normalizedId,
    name: pill.name ?? pill.display_name ?? pill.title ?? '',
    activeIngredient: pill.activeIngredient ?? pill.active_ingredient ?? '',
    dosage: pill.dosage ?? pill.normalized_dosage ?? '',
    manufacturer: pill.manufacturer ?? pill.manufacturer_name ?? pill.brand_name ?? '',
    imageUrls:
      pill.imageUrls ??
      (pill.primary_image_url ? [pill.primary_image_url] : pill.image_url ? [pill.image_url] : []),
  }
}

function normalizePillList(data) {
  const pillsById = new Map()

  asPageContent(data)
    .map((pill) => normalizeCatalogPill(pill))
    .filter((pill) => pill?.id)
    .forEach((pill) => {
      const id = normalizePillId(pill.id)
      if (!pillsById.has(id)) {
        pillsById.set(id, pill)
      }
    })

  return [...pillsById.values()]
}

export async function searchPills(keyword, { limit = 10 } = {}) {
  const text = keyword?.trim()

  if (!text) {
    return []
  }

  let primaryError = null

  try {
    const response = await apiClient.get('/pills/search', {
      params: {
        keyword: text,
        limit,
      },
    })
    const results = normalizePillList(response.data)
    if (results.length) {
      return results
    }
  } catch (error) {
    primaryError = error
  }

  try {
    const response = await apiClient.get('/pills', {
      params: {
        search: text,
        page: 0,
        size: limit,
      },
    })
    return normalizePillList(response.data)
  } catch (error) {
    if (primaryError) {
      throw primaryError
    }
    throw error
  }
}

export async function getPillById(pillId) {
  const normalizedId = normalizePillId(pillId)
  const response = await apiClient.get(`/pills/${encodeURIComponent(normalizedId)}`)
  return normalizeCatalogPill(response.data, normalizedId)
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

export async function analyzeMedicationText(text, { topK = 8 } = {}) {
  const response = await apiClient.post('/agent/search', {
    ocr_text: text,
    top_k: topK,
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
