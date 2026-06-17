package com.tsd.csm.core.common.enums;

/**
 * 工单完结方式。对应 csm_ticket.close_type。
 */
public enum CloseType {

    USER_RESOLVED(1, "用户已解决"),
    AUTO_TIMEOUT(2, "超时自动完结"),
    AGENT_FORCE(3, "客服强制关闭");

    /** 编码值，对应数据库列。 */
    private final int code;
    /** 中文描述。 */
    private final String desc;

    CloseType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
