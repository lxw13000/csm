package com.tsd.csm.modules.ticket.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 服务评价。对应 csm_ticket_evaluation。
 */
@TableName("csm_ticket_evaluation")
public class TicketEvaluation extends BaseEntity {

    /** 所属租户。 */
    private String appId;
    /** 工单 id。 */
    private Long ticketId;
    /** 是否已解决：1 已解决 / 0 未解决。 */
    private Integer resolved;
    /** 满意度评分 1-5。 */
    private Integer rating;
    /** 评价文字。 */
    private String remark;

    /** 评价时间。 */
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

    public Integer getResolved() {
        return resolved;
    }

    public void setResolved(Integer resolved) {
        this.resolved = resolved;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
