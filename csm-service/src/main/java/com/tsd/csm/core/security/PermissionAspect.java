package com.tsd.csm.core.security;

import com.tsd.csm.core.common.enums.AccountType;
import com.tsd.csm.core.common.exception.BizException;
import com.tsd.csm.core.common.result.ResultCode;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * 权限/角色校验切面。
 *
 * <p>规则：平台超管放行一切；租户管理员若未配置受限角色（权限集为空）视为本租户全权放行，
 * 否则按权限点校验；客服按权限点校验。
 */
@Aspect
@Component
public class PermissionAspect {

    private final PermissionLoader permissionLoader;

    public PermissionAspect(PermissionLoader permissionLoader) {
        this.permissionLoader = permissionLoader;
    }

    @Before("@annotation(requireRole)")
    public void checkRole(RequireRole requireRole) {
        LoginUser user = UserContext.required();
        boolean allowed = Arrays.stream(requireRole.value())
                .anyMatch(t -> user.getAccountType() != null && t.getCode() == user.getAccountType());
        if (!allowed) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    @Before("@annotation(requiresPermission)")
    public void checkPermission(RequiresPermission requiresPermission) {
        LoginUser user = UserContext.required();
        if (user.isPlatformSuper()) {
            return;
        }
        Set<String> perms = permissionLoader.loadPermCodes(user.getAccountId(), user.getAppId());
        if (perms.isEmpty() && user.getAccountType() != null
                && user.getAccountType() == AccountType.TENANT_ADMIN.getCode()) {
            // 租户管理员未配置受限角色 ⇒ 本租户全权
            return;
        }
        if (!perms.contains(requiresPermission.value())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }
}
