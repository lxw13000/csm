import request from './request'
import type {
  AccountSaveDTO, AccountVO, AgentStatVO, CustomerVO, LoginDTO, LoginVO, Menu, MenuSaveDTO,
  MenuVO, OperationLog, PageResult, QaSaveDTO, QaVO, Role, RoleSaveDTO, Tenant, TenantConfig,
  TenantSaveDTO, TicketStatsVO, TicketVO, MessageVO
} from '@/types/api'

// 响应拦截器已解包 R<T>，这里直接以 data 类型返回
function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return request.get(url, { params }) as unknown as Promise<T>
}
function post<T>(url: string, data?: unknown, params?: Record<string, unknown>): Promise<T> {
  return request.post(url, data, { params }) as unknown as Promise<T>
}
function put<T>(url: string, data?: unknown, params?: Record<string, unknown>): Promise<T> {
  return request.put(url, data, { params }) as unknown as Promise<T>
}
function del<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return request.delete(url, { params }) as unknown as Promise<T>
}

/* ===================== 认证 ===================== */
export const login = (dto: LoginDTO) => post<LoginVO>('/admin/auth/login', dto)
export const me = () => get<LoginVO>('/admin/auth/me')
export const logout = () => post<void>('/admin/auth/logout')

/* ===================== 租户接入（平台超管）===================== */
export const tenantPage = (params: Record<string, unknown>) => get<PageResult<Tenant>>('/admin/tenant/page', params)
export const tenantGet = (id: number) => get<Tenant>(`/admin/tenant/${id}`)
export const tenantCreate = (dto: TenantSaveDTO) => post<Tenant>('/admin/tenant', dto)
export const tenantUpdate = (id: number, dto: TenantSaveDTO) => put<Tenant>(`/admin/tenant/${id}`, dto)
export const tenantStatus = (id: number, status: number) => put<void>(`/admin/tenant/${id}/status`, null, { status })

/* ===================== 账号管理 ===================== */
export const accountPage = (params: Record<string, unknown>) => get<PageResult<AccountVO>>('/admin/account/page', params)
export const accountGet = (id: number) => get<AccountVO>(`/admin/account/${id}`)
export const agentsSimple = () => get<AccountVO[]>('/admin/account/agents')
export const accountCreate = (dto: AccountSaveDTO) => post<AccountVO>('/admin/account', dto)
export const accountUpdate = (id: number, dto: AccountSaveDTO) => put<AccountVO>(`/admin/account/${id}`, dto)
export const accountResetPwd = (id: number, password: string) => put<void>(`/admin/account/${id}/password`, null, { password })
export const accountStatus = (id: number, status: number) => put<void>(`/admin/account/${id}/status`, null, { status })

/* ===================== 菜单（平台超管）===================== */
export const menuTree = () => get<MenuVO[]>('/admin/menu/tree')
export const menuCreate = (dto: MenuSaveDTO) => post<Menu>('/admin/menu', dto)
export const menuUpdate = (id: number, dto: MenuSaveDTO) => put<Menu>(`/admin/menu/${id}`, dto)
export const menuDelete = (id: number) => del<void>(`/admin/menu/${id}`)

/* ===================== 角色权限 ===================== */
export const roleList = () => get<Role[]>('/admin/role/list')
export const roleCreate = (dto: RoleSaveDTO) => post<Role>('/admin/role', dto)
export const roleUpdate = (id: number, dto: RoleSaveDTO) => put<Role>(`/admin/role/${id}`, dto)
export const roleDelete = (id: number) => del<void>(`/admin/role/${id}`)
export const roleMenus = (id: number) => get<number[]>(`/admin/role/${id}/menus`)
export const roleSetMenus = (id: number, menuIds: number[]) => put<void>(`/admin/role/${id}/menus`, menuIds)

/* ===================== 接入配置 ===================== */
export const configGet = () => get<TenantConfig>('/admin/config')
export const configUpdate = (dto: TenantConfig) => put<TenantConfig>('/admin/config', dto)

/* ===================== QA 知识库 ===================== */
export const qaPage = (params: Record<string, unknown>) => get<PageResult<QaVO>>('/admin/qa/page', params)
export const qaGet = (id: number) => get<QaVO>(`/admin/qa/${id}`)
export const qaCreate = (dto: QaSaveDTO) => post<QaVO>('/admin/qa', dto)
export const qaUpdate = (id: number, dto: QaSaveDTO) => put<QaVO>(`/admin/qa/${id}`, dto)
export const qaStatus = (id: number, status: number) => put<void>(`/admin/qa/${id}/status`, null, { status })
export const qaDelete = (id: number) => del<void>(`/admin/qa/${id}`)

/* ===================== 统计 ===================== */
export const statsTicket = (params: Record<string, unknown>) => get<TicketStatsVO>('/admin/stats/ticket', params)
export const statsAgent = (params: Record<string, unknown>) => get<AgentStatVO[]>('/admin/stats/agent', params)
/** 手动触发客服工作日汇总（定时任务的聚合内容），返回已聚合天数。 */
export const statsAgentAggregate = (params: Record<string, unknown>) =>
  post<number>('/admin/stats/agent/aggregate', null, params)

/* ===================== 审计日志 ===================== */
export const logPage = (params: Record<string, unknown>) => get<PageResult<OperationLog>>('/admin/log/page', params)

/* ===================== C 端用户 ===================== */
export const customerPage = (params: Record<string, unknown>) => get<PageResult<CustomerVO>>('/admin/customer/page', params)
export const customerDetail = (userId: string) => get<CustomerVO>('/admin/customer/detail', { userId })

/* ===================== 工单（管理端只读）===================== */
export const ticketPage = (params: Record<string, unknown>) => get<PageResult<TicketVO>>('/admin/ticket/page', params)
export const ticketMessages = (id: number, afterSeq?: number) => get<MessageVO[]>(`/admin/ticket/${id}/messages`, afterSeq != null ? { afterSeq } : undefined)
