package com.tsd.csm.core.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限点校验。标注在 controller 方法上，由 {@link PermissionAspect} 拦截校验。
 * 取值对应 csm_menu.perm_code，如 {@code "ticket:list"}。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    /** 所需权限点编码。 */
    String value();
}
