import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

import { env, getHttpEndpoint, getWebSocketEndpoint } from '../config/env.js'

export function createStompClient({
  accessToken,
  onConnect,
  onDisconnect,
  onReady,
  onError,
  useSockJs = true,
} = {}) {
  const connectHeaders = accessToken ? { Authorization: `Bearer ${accessToken}` } : {}
  const clientOptions = {
    connectHeaders,
    debug: () => undefined,
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: (frame) => {
      onConnect?.(frame)
    },
    onDisconnect,
    onStompError: onError,
    onWebSocketError: onError,
  }

  const client = useSockJs
    ? new Client({
      ...clientOptions,
      webSocketFactory: () => new SockJS(getHttpEndpoint(env.wsBaseUrl)),
    })
    : new Client({
        ...clientOptions,
        brokerURL: getWebSocketEndpoint(env.wsBaseUrl),
      })

  client.onConnect = (frame) => {
    onConnect?.(frame)
    onReady?.(client)
  }

  return client
}

export function subscribeToUserQueue(client, destination, callback) {
  if (!client?.connected) {
    return undefined
  }

  return client.subscribe(destination, (message) => {
    const body = message.body ? JSON.parse(message.body) : null
    callback(body, message)
  })
}
