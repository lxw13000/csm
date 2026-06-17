package com.tsd.csm.core.common.enums;

/**
 * WebSocket 逻辑通道类型（单连接，按消息 {@code type} 字段区分）。对应 xuqiu.md 4.5。
 */
public enum WsChannelType {

    /** 文本/多媒体聊天消息收发（双向）。 */
    CHAT("chat"),
    /** 对端「正在输入」状态（双向）。 */
    TYPING("typing"),
    /** 消息已读回执（双向）。 */
    READ("read"),
    /** 新工单分发、新消息提醒（下行）。 */
    NOTIFICATION("notification"),
    /** 工单状态变更（下行）。 */
    TICKET_STATUS("ticket_status"),
    /** 心跳（双向）。 */
    PING("ping"),
    /** 心跳应答（下行）。 */
    PONG("pong"),
    /** 消息送达确认（双向）。 */
    ACK("ack");

    /** 通道标识（消息 type 字段值）。 */
    private final String type;

    WsChannelType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    /** 按 type 字符串查枚举，未匹配返回 null。 */
    public static WsChannelType of(String type) {
        if (type == null) {
            return null;
        }
        for (WsChannelType t : values()) {
            if (t.type.equals(type)) {
                return t;
            }
        }
        return null;
    }
}
