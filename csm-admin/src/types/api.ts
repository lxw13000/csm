// 与后端 csm-service 对齐的类型定义（统一响应体、分页、各 VO/DTO）

export interface R<T> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface PageQuery {
  current?: number
  size?: number
}

export interface MenuVO {
  id: number
  parentId: number
  name: string
  type: number // 1 目录 / 2 菜单 / 3 按钮
  permCode?: string
  path?: string
  sort?: number
  children?: MenuVO[]
}

export interface LoginVO {
  token?: string
  accountId: number
  appId: string
  username: string
  realName: string
  accountType: number // 1 平台超管 / 2 租户管理员 / 3 客服
  permCodes: string[]
  menus: MenuVO[]
}

export interface LoginDTO {
  appId: string
  username: string
  password: string
}

export interface AccountVO {
  id: number
  appId: string
  username: string
  realName?: string
  accountType: number
  status: number
  createdAt?: string
  roleIds?: number[]
}

export interface AccountSaveDTO {
  username: string
  password?: string
  realName?: string
  accountType: number
  status?: number
  roleIds?: number[]
}

export interface Role {
  id: number
  appId?: string
  name: string
  code: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface RoleSaveDTO {
  name: string
  code: string
  remark?: string
  menuIds?: number[]
}

export interface Menu {
  id: number
  parentId: number
  name: string
  type: number
  permCode?: string
  path?: string
  sort?: number
}

export interface MenuSaveDTO {
  parentId?: number
  name: string
  type: number
  permCode?: string
  path?: string
  sort?: number
}

export interface Tenant {
  id: number
  appId: string
  appSecret: string
  name: string
  identityApi: string
  userInfoApi: string
  ipWhitelist?: string
  status: number
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface TenantSaveDTO {
  appId: string
  appSecret: string
  name: string
  identityApi: string
  userInfoApi: string
  ipWhitelist?: string
  status?: number
  remark?: string
}

export interface TenantConfig {
  id?: number
  appId?: string
  maxConcurrent: number
  autoCloseMinutes: number
  notifySound?: number
  ext?: string
  createdAt?: string
  updatedAt?: string
}

export interface QaVO {
  id: number
  question: string
  answer: string
  status: number
  keywords?: string[]
  createdAt?: string
}

export interface QaSaveDTO {
  question: string
  answer: string
  status?: number
  keywords?: string[]
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
  senderType: number // 1 用户 / 2 客服 / 3 系统
  senderId?: string
  contentType: number // 1 文本 / 2 图片 / 3 其他
  content: string
  responseCost?: number
  createdAt?: string
}

export interface TicketStatsVO {
  total: number
  qa: number
  transferring: number
  processing: number
  closed: number
  closeRate: number
  avgHandleSeconds: number
}

export interface AgentStatVO {
  agentId: number
  realName?: string
  onlineSeconds: number
  ticketCount: number
  replyCount: number
  avgResponseCost: number
  forceCloseCount: number
}

export interface OperationLog {
  id: number
  appId?: string
  operatorId?: number
  operatorName?: string
  operatorType?: number
  module?: string
  action?: string
  targetType?: string
  targetId?: string
  detail?: string
  clientIp?: string
  userAgent?: string
  createdAt?: string
}
