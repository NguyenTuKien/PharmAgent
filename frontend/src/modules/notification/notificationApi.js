import { apiClient } from '../../lib/apiClient.js'

export async function getNotifications({ page = 0, size = 10 } = {}) {
  const response = await apiClient.get('/notifications', {
    params: { page, size },
  })
  return response.data
}

export async function getSentNotifications({ page = 0, size = 10 } = {}) {
  const response = await apiClient.get('/notifications/sent', {
    params: { page, size },
  })
  return response.data
}

export async function sendNotificationRequest({ receiverId, content }) {
  const response = await apiClient.post('/notifications', {
    receiverId,
    content,
  })
  return response.data
}
