package com.tsd.csm.core.common.enums;

/**
 * 已读水位阅读方类型。对应 csm_message_read.reader_type。
 */
public enum ReaderType {

    USER(1, "用户"),
    AGENT(2, "客服");

    /** 编码值，对应数据库列。 */
    private final int code;
    /** 中文描述。 */
    private final String desc;

    ReaderType(int code, String desc) {
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
