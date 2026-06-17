package com.tsd.csm.core.security;

import com.tsd.csm.core.common.exception.BizException;
import com.tsd.csm.core.common.result.ResultCode;

/**
 * 当前登录用户上下文（线程级）。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 获取当前登录用户，未登录抛 401。 */
    public static LoginUser required() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return u;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
