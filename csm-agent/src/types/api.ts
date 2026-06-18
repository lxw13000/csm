// 客服端类型定义（与 csm-service 对齐）

export interface R<T> {
  code: number
  msg: string
  data: T
}

export interface LoginDTO {
  appId: string
  username: string
  password: string
}

export interface LoginVO {
  token?: string
  accountId: number
  appId: string
  username: string
  realName: string
  accountType: number
}

export interface AgentStatus {
  id?: number
  appId?: string
  accountId?: number
  onlineStatus: number // 0 离线 / 1 在线
  currentLoad?: number
  lastOnlineAt?: string
  updatedAt?: string
}

export interface TicketVO {
  id: number
  userId: string
  nickname?: string
  avatar?: string
  status: number
  closeType?: number
  agentId?: number
  unreadCount?: number
  firstMsgAt?: string
  lastMsgAt?: string
  closedAt?: string
  createdAt?: string
}

export interface MessageVO {
  id: number
  ticketId: number
  seq: number
  senderType: number // 1 用户 / 2 客服 / 3 系统
  senderId?: string
  contentType: number // 1 文本 / 2 图片 / 3 其他
  content: string
  responseCost?: number
  createdAt?: string
  // 本地态：发送中/已读（不来自后端）
  _pending?: boolean
  _clientMsgId?: string
}

export interface SendMessageDTO {
  content: string
  contentType?: number
  clientMsgId?: string
}

export interface TransferDTO {
  toAgentId: number
  reason?: string
}

/** 可转接客服（来自 /agent/ticket/transfer-targets）。 */
export interface AccountBrief {
  id: number
  username: string
  realName?: string
  accountType?: number
  status?: number
}

/** 文件上传结果。 */
export interface UploadVO {
  url: string
  name: string
  size: number
}

/** WebSocket 下行消息（服务端推送统一为 { type, data }，直发的 ack/pong 含顶层字段）。 */
export interface WsInbound {
  type: string
  ticketId?: number
  id?: number
  clientMsgId?: string
  seq?: number
  data?: any
}
