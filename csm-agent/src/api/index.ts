import request from './request'
import type { AgentStatus, LoginDTO, LoginVO, MessageVO, SendMessageDTO, TicketVO, TransferDTO } from '@/types/api'

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
export const ticketMessages = (id: number, afterSeq?: number) =>
  get<MessageVO[]>(`/agent/ticket/${id}/messages`, afterSeq != null ? { afterSeq } : undefined)
export const reply = (id: number, dto: SendMessageDTO) => post<MessageVO>(`/agent/ticket/${id}/reply`, dto)
export const transfer = (id: number, dto: TransferDTO) => post<unknown>(`/agent/ticket/${id}/transfer`, dto)
export const closeTicket = (id: number) => post<unknown>(`/agent/ticket/${id}/close`)
export const markRead = (id: number, seq: number) => post<void>(`/agent/ticket/${id}/read`, null, { seq })
