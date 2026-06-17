package com.tsd.csm.modules.stats.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.TimedEntity;

import java.time.LocalDate;

/**
 * 客服工作情况日汇总（按日预聚合）。对应 csm_agent_work_daily。
 */
@TableName("csm_agent_work_daily")
public class AgentWorkDaily extends TimedEntity {

    /** 所属租户。 */
    private String appId;
    /** 客服账号 id（csm_account.id）。 */
    private Long agentId;
    /** 统计日期。 */
    private LocalDate statDate;
    /** 在线时长（秒）。 */
    private Integer onlineSeconds;
    /** 接待工单数。 */
    private Integer ticketCount;
    /** 回复消息数。 */
    private Integer replyCount;
    /** 平均响应耗时（秒）。 */
    private Integer avgResponseCost;
    /** 强制关闭工单数。 */
    private Integer forceCloseCount;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public Integer getOnlineSeconds() {
        return onlineSeconds;
    }

    public void setOnlineSeconds(Integer onlineSeconds) {
        this.onlineSeconds = onlineSeconds;
    }

    public Integer getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(Integer ticketCount) {
        this.ticketCount = ticketCount;
    }

    public Integer getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    public Integer getAvgResponseCost() {
        return avgResponseCost;
    }

    public void setAvgResponseCost(Integer avgResponseCost) {
        this.avgResponseCost = avgResponseCost;
    }

    public Integer getForceCloseCount() {
        return forceCloseCount;
    }

    public void setForceCloseCount(Integer forceCloseCount) {
        this.forceCloseCount = forceCloseCount;
    }
}
