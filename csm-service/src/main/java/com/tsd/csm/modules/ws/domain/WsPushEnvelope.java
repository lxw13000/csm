package com.tsd.csm.modules.ws.domain;

/**
 * 跨节点下行推送信封：经 Redis 频道广播，各节点据此定位本地连接投递。
 */
public class WsPushEnvelope {

    /** 目标类型：user / agent。 */
    private String targetType;
    /** 所属租户。 */
    private String appId;
    /** 目标标识：user_id 或客服 accountId（字符串）。 */
    private String targetId;
    /** WebSocket 消息 type（chat/typing/read/notification/ticket_status...）。 */
    private String type;
    /** 消息载荷。 */
    private Object payload;

    /** 空构造（反序列化用）。 */
    public WsPushEnvelope() {
    }

    /** 全参构造。 */
    public WsPushEnvelope(String targetType, String appId, String targetId, String type, Object payload) {
        this.targetType = targetType;
        this.appId = appId;
        this.targetId = targetId;
        this.type = type;
        this.payload = payload;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
