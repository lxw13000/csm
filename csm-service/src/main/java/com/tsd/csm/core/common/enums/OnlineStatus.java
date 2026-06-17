package com.tsd.csm.core.common.enums;

/**
 * 客服在线状态。对应 csm_agent_status.online_status。
 */
public enum OnlineStatus {

    OFFLINE(0, "离线"),
    ONLINE(1, "在线");

    /** 编码值，对应数据库列。 */
    private final int code;
    /** 中文描述。 */
    private final String desc;

    OnlineStatus(int code, String desc) {
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
