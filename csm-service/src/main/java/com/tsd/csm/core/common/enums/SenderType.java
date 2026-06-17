package com.tsd.csm.core.common.enums;

/**
 * 消息发送方类型。对应 csm_message.sender_type。
 */
public enum SenderType {

    USER(1, "用户"),
    AGENT(2, "客服"),
    SYSTEM(3, "系统/机器人");

    /** 编码值，对应数据库列。 */
    private final int code;
    /** 中文描述。 */
    private final String desc;

    SenderType(int code, String desc) {
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
