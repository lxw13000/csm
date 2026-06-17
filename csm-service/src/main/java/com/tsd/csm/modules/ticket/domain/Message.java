package com.tsd.csm.modules.ticket.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 会话消息（毫秒时间精度）。对应 csm_message。
 * response_cost 由服务端落库时按「本条客服消息时间 − 上一条用户消息时间」计算。
 */
@TableName("csm_message")
public class Message extends BaseEntity {

    /** 所属租户。 */
    private String appId;
    /** 工单 id。 */
    private Long ticketId;
    /** 客户端生成唯一 id，用于去重（4.5）。 */
    private String clientMsgId;
    /** 会话内递增序号，用于排序与断线增量恢复。 */
    private Long seq;
    /** 发送方：1 用户 / 2 客服 / 3 系统/机器人。 */
    private Integer senderType;
    /** 发送方标识（user_id 或客服 account_id）。 */
    private String senderId;
    /** 内容类型：1 文本 / 2 图片 / 3 其他多媒体。 */
    private Integer contentType;
    /** 文本内容或媒体引用 URL。 */
    private String content;
    /** 响应耗时（秒），仅客服回复且上一条为用户消息时记录。 */
    private Integer responseCost;

    /** 发送时间（毫秒精度）。 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
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
