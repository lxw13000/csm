import type { WsInbound } from '@/types/api'

type MessageHandler = (msg: WsInbound) => void

/**
 * WebSocket 客户端：心跳保活 + 指数退避重连（呼应 xuqiu.md 4.5、5.5）。
 * 单连接，按消息 type 区分逻辑通道；连接建立后回调 { type: '__open' } 供上层做增量恢复。
 */
export class WsClient {
  private ws?: WebSocket
  private readonly url: string
  private readonly handler: MessageHandler
  private heartbeatTimer?: number
  private reconnectTimer?: number
  private retries = 0
  private manualClosed = false

  constructor(token: string, handler: MessageHandler) {
    this.handler = handler
    const proto = location.protocol === 'https:' ? 'wss' : 'ws'
    this.url = `${proto}://${location.host}/ws?token=${encodeURIComponent(token)}`
  }

  connect() {
    this.manualClosed = false
    this.open()
  }

  private open() {
    this.ws = new WebSocket(this.url)
    this.ws.onopen = () => {
      this.retries = 0
      this.startHeartbeat()
      this.handler({ type: '__open' })
    }
    this.ws.onmessage = (e) => {
      try {
        const msg = JSON.parse(e.data) as WsInbound
        if (msg.type === 'pong') return
        this.handler(msg)
      } catch {
        /* 忽略非 JSON 帧 */
      }
    }
    this.ws.onclose = () => {
      this.stopHeartbeat()
      if (!this.manualClosed) this.scheduleReconnect()
    }
    this.ws.onerror = () => this.ws?.close()
  }

  send(obj: Record<string, unknown>): boolean {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(obj))
      return true
    }
    return false
  }

  isOpen(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }

  private startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = window.setInterval(() => this.send({ type: 'ping' }), 25000)
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = undefined
    }
  }

  private scheduleReconnect() {
    const delay = Math.min(1000 * 2 ** this.retries, 30000)
    this.retries++
    this.reconnectTimer = window.setTimeout(() => this.open(), delay)
  }

  close() {
    this.manualClosed = true
    this.stopHeartbeat()
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    this.ws?.close()
  }
}
