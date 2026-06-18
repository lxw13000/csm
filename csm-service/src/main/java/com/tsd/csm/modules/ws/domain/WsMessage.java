package com.tsd.csm.modules.ws.domain;

/**
 * WebSocket 消息体（单连接，按 {@code type} 区分逻辑通道，呼应 xuqiu.md 4.5）。
 * 入站与下行复用同一结构，未用字段序列化时忽略（全局 non_null）。
 */
public class WsMessage {

    /** 消息类型/逻辑通道：chat/typing/read/notification/ticket_status/ping/pong/ack。 */
    private String type;
    /** 工单 id。 */
    private Long ticketId;
    /** 服务端分配的消息主键 id（ack 回传，供前端去重/排序）。 */
    private Long id;
    /** 客户端生成的唯一 id，用于去重与 ack 关联。 */
    private String clientMsgId;
    /** 会话内递增序号。 */
    private Long seq;
    /** 内容类型：1 文本 / 2 图片 / 3 其他多媒体。 */
    private Integer contentType;
    /** 文本内容或媒体引用 URL。 */
    private String content;
    /** 通用数据载荷（下行推送时承载 MessageVO、状态等）。 */
    private Object data;

    public WsMessage() {
    }

    /** 构造仅含 type + data 的下行消息。 */
    public static WsMessage of(String type, Object data) {
        WsMessage message = new WsMessage();
        message.type = type;
        message.data = data;
        return message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientMsgId() {
        return clientMsgId;
    }

    public void setClientMsgId(String clientMsgId) {
        this.clientMsgId = clientMsgId;
    }

    public Long getSeq() {
        return seq;
    }

    public void setSeq(Long seq) {
        this.seq = seq;
    }

    public Integer getContentType() {
        return contentType;
    }

    public void setContentType(Integer contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
