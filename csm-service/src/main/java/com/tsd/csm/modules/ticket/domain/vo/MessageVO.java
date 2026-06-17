package com.tsd.csm.modules.ticket.domain.vo;

import java.time.LocalDateTime;

/**
 * 会话消息展示 VO。
 */
public class MessageVO {

    /** 消息 id。 */
    private Long id;
    /** 工单 id。 */
    private Long ticketId;
    /** 会话内递增序号。 */
    private Long seq;
    /** 发送方：1 用户 / 2 客服 / 3 系统/机器人。 */
    private Integer senderType;
    /** 发送方标识（user_id 或客服 account_id）。 */
    private String senderId;
    /** 内容类型：1 文本 / 2 图片 / 3 其他多媒体。 */
    private Integer contentType;
    /** 文本内容或媒体引用 URL。 */
    private String content;
    /** 响应耗时（秒），仅客服回复且上一条为用户消息时有值。 */
    private Integer responseCost;
    /** 发送时间。 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getSeq() {
        return seq;
    }

    public void setSeq(Long seq) {
        this.seq = seq;
    }

    public Integer getSenderType() {
        return senderType;
    }

    public void setSenderType(Integer senderType) {
        this.senderType = senderType;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
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

    public Integer getResponseCost() {
        return responseCost;
    }

    public void setResponseCost(Integer responseCost) {
        this.responseCost = responseCost;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
