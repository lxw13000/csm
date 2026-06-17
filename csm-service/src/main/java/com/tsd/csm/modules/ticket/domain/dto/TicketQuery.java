package com.tsd.csm.modules.ticket.domain.dto;

import com.tsd.csm.core.common.query.PageQuery;

/**
 * 工单分页查询入参（管理端本租户）。
 */
public class TicketQuery extends PageQuery {

    /** 按 C 端用户 id 筛选。 */
    private String userId;
    /** 按状态筛选：1 智能问答 / 2 人工转接中 / 3 处理中 / 4 已完结。 */
    private Integer status;
    /** 按处理客服账号 id 筛选。 */
    private Long agentId;

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

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }
}
