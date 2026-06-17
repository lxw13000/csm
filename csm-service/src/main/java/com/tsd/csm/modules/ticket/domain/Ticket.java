package com.tsd.csm.modules.ticket.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.TimedEntity;

import java.time.LocalDateTime;

/**
 * 工单。对应 csm_ticket。
 * status：1 智能问答 / 2 人工转接中 / 3 处理中 / 4 已完结；close_type：1 用户已解决 / 2 超时自动 / 3 客服强制。
 */
@TableName("csm_ticket")
public class Ticket extends TimedEntity {

    /** 所属租户。 */
    private String appId;
    /** 所属 C 端用户（业务系统 user_id）。 */
    private String userId;
    /** 状态：1 智能问答 / 2 人工转接中 / 3 处理中 / 4 已完结。 */
    private Integer status;
    /** 完结方式：1 用户已解决 / 2 超时自动 / 3 客服强制。 */
    private Integer closeType;
    /** 当前处理客服账号 id。 */
    private Long agentId;
    /** 首条消息时间。 */
    private LocalDateTime firstMsgAt;
    /** 派单时间。 */
    private LocalDateTime assignedAt;
    /** 最后一条消息时间。 */
    private LocalDateTime lastMsgAt;
    /** 完结时间。 */
    private LocalDateTime closedAt;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getCloseType() {
        return closeType;
    }

    public void setCloseType(Integer closeType) {
        this.closeType = closeType;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public LocalDateTime getFirstMsgAt() {
        return firstMsgAt;
    }

    public void setFirstMsgAt(LocalDateTime firstMsgAt) {
        this.firstMsgAt = firstMsgAt;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getLastMsgAt() {
        return lastMsgAt;
    }

    public void setLastMsgAt(LocalDateTime lastMsgAt) {
        this.lastMsgAt = lastMsgAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
