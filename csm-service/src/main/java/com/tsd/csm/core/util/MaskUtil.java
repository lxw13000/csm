package com.tsd.csm.core.util;

import org.springframework.util.StringUtils;

/**
 * 脱敏工具。C 端用户手机号等仅以脱敏形式缓存（呼应 db-schema.md 通用约定）。
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    /** 手机号脱敏：保留前 3 后 4，中间以 **** 替代。 */
    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        int len = phone.length();
        return phone.substring(0, 3) + "****" + phone.substring(len - 4);
    }
}
