package com.tsd.csm.modules.ticket.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 消息已读水位（read 回执）。对应 csm_ticket_message_read。
 * 高水位设计：每个 (工单, 阅读方) 仅一行，随阅读推进更新 last_read_seq。
 */
@TableName("csm_ticket_message_read")
public class MessageRead extends BaseEntity {

    /** 所属租户。 */
    private String appId;
    /** 工单 id。 */
    private Long ticketId;
    /** 阅读方：1 用户 / 2 客服。 */
    private Integer readerType;
    /** 阅读方标识（user_id 或客服 account_id）。 */
    private String readerId;
    /** 已读到的最大消息序号（对应 csm_ticket_message.seq）。 */
    private Long lastReadSeq;
    /** 最近已读时间。 */
    private LocalDateTime lastReadAt;

    /** 更新时间。 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

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

    public Integer getReaderType() {
        return readerType;
    }

    public void setReaderType(Integer readerType) {
        this.readerType = readerType;
    }

    public String getReaderId() {
        return readerId;
    }

    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }

    public Long getLastReadSeq() {
        return lastReadSeq;
    }

    public void setLastReadSeq(Long lastReadSeq) {
        this.lastReadSeq = lastReadSeq;
    }

    public LocalDateTime getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
