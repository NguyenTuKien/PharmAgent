import { apiClient } from '../../lib/apiClient.js'

export async function getChatRooms() {
  const { data } = await apiClient.get('/chat/rooms')
  return data
}

export async function createDirectRoom(targetProfileId) {
  const { data } = await apiClient.post(`/chat/rooms/direct/${targetProfileId}`)
  return data
}

export async function getRoomMessages(roomId, { page = 0, size = 50 } = {}) {
  const { data } = await apiClient.get(`/chat/rooms/${roomId}/messages`, {
    params: { page, size },
  })
  return data
}

export async function markRoomRead(roomId) {
  await apiClient.post(`/chat/rooms/${roomId}/read`)
}

export async function sendChatMessage(roomId, { content, type = 'TEXT' }) {
  const { data } = await apiClient.post(`/chat/rooms/${roomId}/messages`, {
    content,
    type,
  })
  return data
}
