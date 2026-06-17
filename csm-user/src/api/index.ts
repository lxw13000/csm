import request from './request'
import type {
  AccessDTO, AccessVO, EvaluateDTO, MessageVO, SendMessageDTO, TicketVO, UserMessageResultVO
} from '@/types/api'

function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return request.get(url, { params }) as unknown as Promise<T>
}
function post<T>(url: string, data?: unknown, params?: Record<string, unknown>): Promise<T> {
  return request.post(url, data, { params }) as unknown as Promise<T>
}

/* 接入：token 换取会话凭证（公开接口） */
export const access = (dto: AccessDTO) => post<AccessVO>('/h5/access', dto)

/* 会话与消息 */
export const currentTicket = () => get<TicketVO>('/h5/ticket/current')
export const messages = (ticketId: number, afterSeq?: number) =>
  get<MessageVO[]>('/h5/ticket/messages', afterSeq != null ? { ticketId, afterSeq } : { ticketId })
export const sendMessage = (dto: SendMessageDTO) => post<UserMessageResultVO>('/h5/message', dto)
export const requestHuman = () => post<TicketVO>('/h5/transfer')
export const resolve = () => post<TicketVO>('/h5/resolve')
export const unresolved = () => post<TicketVO>('/h5/unresolved')
export const evaluate = (dto: EvaluateDTO) => post<void>('/h5/evaluate', dto)
export const markRead = (ticketId: number, seq: number) =>
  post<void>('/h5/ticket/read', null, { ticketId, seq })
