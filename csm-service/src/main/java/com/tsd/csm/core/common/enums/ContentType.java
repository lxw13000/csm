package com.tsd.csm.core.common.enums;

/**
 * 消息内容类型。对应 csm_message.content_type。
 */
public enum ContentType {

    TEXT(1, "文本"),
    IMAGE(2, "图片"),
    OTHER(3, "其他多媒体");

    /** 编码值，对应数据库列。 */
    private final int code;
    /** 中文描述。 */
    private final String desc;

    ContentType(int code, String desc) {
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
