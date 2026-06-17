package com.tsd.csm.modules.ticket.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 工单转接记录。对应 csm_ticket_transfer。
 */
@TableName("csm_ticket_transfer")
public class TicketTransfer extends BaseEntity {

    /** 所属租户。 */
    private String appId;
    /** 工单 id。 */
    private Long ticketId;
    /** 转出客服账号 id。 */
    private Long fromAgentId;
    /** 转入客服账号 id。 */
    private Long toAgentId;
    /** 转接原因。 */
    private String reason;

    /** 转接时间。 */
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

    public Long getFromAgentId() {
        return fromAgentId;
    }

    public void setFromAgentId(Long fromAgentId) {
        this.fromAgentId = fromAgentId;
    }

    public Long getToAgentId() {
        return toAgentId;
    }

    public void setToAgentId(Long toAgentId) {
        this.toAgentId = toAgentId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
