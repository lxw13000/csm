package com.tsd.csm.core.security;

import com.tsd.csm.core.common.enums.AccountType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色校验。限定接口仅允许指定账号类型访问，如平台超管专属接口。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许访问的账号类型（满足其一即放行）。 */
    AccountType[] value();
}
