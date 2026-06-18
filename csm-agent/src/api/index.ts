import request from './request'
import type { AccountBrief, AgentStatus, LoginDTO, LoginVO, MessageVO, SendMessageDTO, TicketVO, TransferDTO, UploadVO } from '@/types/api'

function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return request.get(url, { params }) as unknown as Promise<T>
}
function post<T>(url: string, data?: unknown, params?: Record<string, unknown>): Promise<T> {
  return request.post(url, data, { params }) as unknown as Promise<T>
}

/* 认证 */
export const login = (dto: LoginDTO) => post<LoginVO>('/agent/auth/login', dto)
export const me = () => get<LoginVO>('/agent/auth/me')
export const logout = () => post<void>('/agent/auth/logout')

/* 上下线 / 状态 */
export const goOnline = () => post<AgentStatus>('/agent/online')
export const goOffline = () => post<AgentStatus>('/agent/offline')
export const getStatus = () => get<AgentStatus>('/agent/status')

/* 工单与会话 */
export const ticketList = () => get<TicketVO[]>('/agent/ticket/list')
export const ticketDetail = (id: number) => get<TicketVO>(`/agent/ticket/${id}`)
/** 客服点开工单 = 接入人工（转接中 -> 处理中）。 */
export const acceptTicket = (id: number) => post<TicketVO>(`/agent/ticket/${id}/accept`)
/** 可转接的本租户客服列表。 */
export const transferTargets = () => get<AccountBrief[]>('/agent/ticket/transfer-targets')
export const ticketMessages = (id: number, afterId?: number) =>
  get<MessageVO[]>(`/agent/ticket/${id}/messages`, afterId != null ? { afterId } : undefined)
/** 历史分页：取 beforeId 之前的最近 limit 条（beforeId 为空取最新 limit 条）。按工单所属用户的全量历史。 */
export const ticketMessagesBefore = (id: number, beforeId?: number, limit = 20) =>
  get<MessageVO[]>(`/agent/ticket/${id}/messages`, beforeId != null ? { beforeId, limit } : { limit })
export const reply = (id: number, dto: SendMessageDTO) => post<MessageVO>(`/agent/ticket/${id}/reply`, dto)
export const transfer = (id: number, dto: TransferDTO) => post<unknown>(`/agent/ticket/${id}/transfer`, dto)
export const closeTicket = (id: number) => post<unknown>(`/agent/ticket/${id}/close`)
export const markRead = (id: number, seq: number) => post<void>(`/agent/ticket/${id}/read`, null, { seq })

/** 上传文件，返回可访问地址（用于发送图片/视频/其他多媒体消息）。 */
export const upload = (file: File): Promise<UploadVO> => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/file/upload', fd) as unknown as Promise<UploadVO>
}
