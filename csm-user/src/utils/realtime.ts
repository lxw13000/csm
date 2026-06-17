import { WsClient } from './ws'
import type { WsInbound } from '@/types/api'

let client: WsClient | null = null
const listeners = new Set<(msg: WsInbound) => void>()

export function initRealtime(token: string) {
  if (client) return
  client = new WsClient(token, (msg) => listeners.forEach((cb) => cb(msg)))
  client.connect()
}

export function onWs(cb: (msg: WsInbound) => void) {
  listeners.add(cb)
  return () => listeners.delete(cb)
}

export function wsSend(obj: Record<string, unknown>): boolean {
  return client?.send(obj) ?? false
}

export function wsOpen(): boolean {
  return client?.isOpen() ?? false
}

export function closeRealtime() {
  client?.close()
  client = null
  listeners.clear()
}
