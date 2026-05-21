import { create } from 'zustand'

import { getNotifications, getSentNotifications, sendNotificationRequest } from './notificationApi.js'

export const useNotificationStore = create((set, get) => ({
  notifications: [],
  sentNotifications: [],
  loading: false,
  loadingSent: false,
  sending: false,
  error: null,

  fetchNotifications: async () => {
    set({ loading: true, error: null })
    try {
      const data = await getNotifications({ page: 0, size: 50 })
      const list = data.content || []
      set({ notifications: list, loading: false })
    } catch (err) {
      set({ error: err, loading: false })
    }
  },

  fetchSentNotifications: async () => {
    set({ loadingSent: true, error: null })
    try {
      const data = await getSentNotifications({ page: 0, size: 50 })
      const list = data.content || []
      set({ sentNotifications: list, loadingSent: false })
    } catch (err) {
      set({ error: err, loadingSent: false })
    }
  },

  sendNotification: async ({ receiverId, content }) => {
    set({ sending: true, error: null })
    try {
      const newNotif = await sendNotificationRequest({ receiverId, content })
      set({ sending: false })
      // Refresh sent notifications list
      get().fetchSentNotifications()
      return newNotif
    } catch (err) {
      set({ error: err, sending: false })
      throw err
    }
  },
}))
