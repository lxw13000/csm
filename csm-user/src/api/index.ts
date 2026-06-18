import request from './request'
import type {
  CredentialDTO, CredentialVO, EvaluateDTO, MessageVO, SendMessageDTO, TicketVO, UploadVO, UserMessageResultVO
} from '@/types/api'

function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return request.get(url, { params }) as unknown as Promise<T>
}
function post<T>(url: string, data?: unknown, params?: Record<string, unknown>): Promise<T> {
  return request.post(url, data, { params }) as unknown as Promise<T>
}

/* 颁发通信凭证（公开接口）。正式环境由业务系统后端调用；此处仅供本地联调直接换取凭证。 */
export const issueCredential = (dto: CredentialDTO) => post<CredentialVO>('/integration/credential', dto)

/* 会话与消息 */
export const currentTicket = () => get<TicketVO>('/h5/ticket/current')
/** 增量拉取：取 id 大于 afterId 的当前用户消息（断线恢复）。 */
export const messages = (afterId?: number) =>
  get<MessageVO[]>('/h5/ticket/messages', afterId != null ? { afterId } : undefined)
/** 历史分页：取 beforeId 之前的最近 limit 条当前用户消息（beforeId 为空取最新 limit 条）。 */
export const messagesBefore = (beforeId?: number, limit = 10) =>
  get<MessageVO[]>('/h5/ticket/messages', beforeId != null ? { beforeId, limit } : { limit })
export const sendMessage = (dto: SendMessageDTO) => post<UserMessageResultVO>('/h5/message', dto)
export const requestHuman = () => post<TicketVO>('/h5/transfer')
export const resolve = () => post<TicketVO>('/h5/resolve')
export const unresolved = () => post<TicketVO>('/h5/unresolved')
export const evaluate = (dto: EvaluateDTO) => post<void>('/h5/evaluate', dto)
export const markRead = (ticketId: number, seq: number) =>
  post<void>('/h5/ticket/read', null, { ticketId, seq })

/** 上传文件，返回可访问地址（用于发送图片/视频/其他多媒体消息）。 */
export const upload = (file: File): Promise<UploadVO> => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/file/upload', fd) as unknown as Promise<UploadVO>
}
