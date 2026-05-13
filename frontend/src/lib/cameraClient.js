import ReconnectingWebSocket from 'reconnecting-websocket'

import { appendQuery, env, getWebSocketEndpoint } from '../config/env.js'

export function createCameraScanClient({ token, onOpen, onClose, onResult, onError } = {}) {
  const url = appendQuery(getWebSocketEndpoint(env.cameraWsPath), { token })
  const socket = new ReconnectingWebSocket(url, [], {
    connectionTimeout: 4000,
    maxRetries: 8,
    minReconnectionDelay: 1000,
    maxReconnectionDelay: 5000,
  })

  socket.addEventListener('open', onOpen)
  socket.addEventListener('close', onClose)
  socket.addEventListener('error', onError)
  socket.addEventListener('message', (event) => {
    try {
      onResult?.(JSON.parse(event.data))
    } catch {
      onResult?.({ raw: event.data })
    }
  })

  return {
    close: () => socket.close(),
    sendFrame: (base64Frame) => {
      if (socket.readyState === WebSocket.OPEN) {
        socket.send(base64Frame)
        return true
      }
      return false
    },
    socket,
  }
}
