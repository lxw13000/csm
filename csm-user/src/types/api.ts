// 用户端 H5 类型定义（与 csm-service 对齐）

export interface R<T> {
  code: number
  msg: string
  data: T
}

export interface AccessDTO {
  appId: string
  token: string
}

export interface CustomerVO {
  userId: string
  nickname?: string
  avatar?: string
  userLevel?: string
  maskedPhone?: string
  registerTime?: string
  lastSyncAt?: string
  latest?: boolean
}

export interface AccessVO {
  sessionToken: string
  appId: string
  userId: string
  customer?: CustomerVO
}

export interface TicketVO {
  id: number
  userId: string
  nickname?: string
  avatar?: string
  status: number // 1 QA / 2 转接中 / 3 处理中 / 4 已完结
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
  senderType: number // 1 用户 / 2 客服 / 3 系统(机器人)
  senderId?: string
  contentType: number
  content: string
  responseCost?: number
  createdAt?: string
  _pending?: boolean
  _clientMsgId?: string
}

export interface SendMessageDTO {
  content: string
  contentType?: number
  clientMsgId?: string
}

export interface UserMessageResultVO {
  ticket: TicketVO
  message: MessageVO
  botReply?: MessageVO
  transferred?: boolean
}

export interface EvaluateDTO {
  resolved?: number
  rating?: number
  remark?: string
}

export interface WsInbound {
  type: string
  ticketId?: number
  clientMsgId?: string
  seq?: number
  data?: any
}
