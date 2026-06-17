import { WsClient } from './ws'
import type { WsInbound } from '@/types/api'

/**
 * 全局单例实时层：跨「会话列表 / 聊天」页面共享同一条 WebSocket。
 * 组件通过 onWs 订阅下行消息，wsSend 发送上行。
 */
let client: WsClient | null = null
const listeners = new Set<(msg: WsInbound) => void>()

export function initRealtime(token: string) {
  if (client) return
  client = new WsClient(token, (msg) => {
    listeners.forEach((cb) => cb(msg))
  })
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
