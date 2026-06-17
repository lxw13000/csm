import { useAuthStore } from '@/stores/auth'

/**
 * 前端权限判断，镜像后端 PermissionAspect：
 * - 平台超管（accountType=1）放行一切；
 * - 租户管理员（accountType=2）未配置受限角色（permCodes 为空）视为本租户全权；
 * - 其余按权限点校验。
 */
export function hasPerm(code: string): boolean {
  const auth = useAuthStore()
  const user = auth.user
  if (!user) return false
  if (user.accountType === 1) return true
  const perms = user.permCodes || []
  if (perms.length === 0 && user.accountType === 2) return true
  return perms.includes(code)
}

/** 仅平台超管可见（租户接入、菜单管理等 @RequireRole(PLATFORM_SUPER) 接口）。 */
export function isPlatformSuper(): boolean {
  return useAuthStore().user?.accountType === 1
}
