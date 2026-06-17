package com.tsd.csm.core.common.enums;

/**
 * 账号类型。对应 csm_account.account_type。
 */
public enum AccountType {

    PLATFORM_SUPER(1, "平台超级管理员"),
    TENANT_ADMIN(2, "租户管理员"),
    AGENT(3, "客服");

    /** 编码值，对应数据库列。 */
    private final int code;
    /** 中文描述。 */
    private final String desc;

    AccountType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 按编码查枚举，未匹配返回 null。 */
    public static AccountType of(Integer code) {
        if (code == null) {
            return null;
        }
        for (AccountType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }
}
