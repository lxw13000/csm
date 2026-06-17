package com.tsd.csm.modules.stats.domain.vo;

/**
 * 客服工作情况统计 VO（按日期范围聚合 csm_agent_work_daily）。
 */
public class AgentStatVO {

    /** 客服账号 id。 */
    private Long agentId;
    /** 客服姓名。 */
    private String realName;
    /** 在线时长（秒）。 */
    private long onlineSeconds;
    /** 接待工单数。 */
    private long ticketCount;
    /** 回复消息数。 */
    private long replyCount;
    /** 平均响应耗时（秒，按回复数加权）。 */
    private int avgResponseCost;
    /** 强制关闭工单数。 */
    private long forceCloseCount;

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public long getOnlineSeconds() {
        return onlineSeconds;
    }

    public void setOnlineSeconds(long onlineSeconds) {
        this.onlineSeconds = onlineSeconds;
    }

    public long getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(long ticketCount) {
        this.ticketCount = ticketCount;
    }

    public long getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(long replyCount) {
        this.replyCount = replyCount;
    }

    public int getAvgResponseCost() {
        return avgResponseCost;
    }

    public void setAvgResponseCost(int avgResponseCost) {
        this.avgResponseCost = avgResponseCost;
    }

    public long getForceCloseCount() {
        return forceCloseCount;
    }

    public void setForceCloseCount(long forceCloseCount) {
        this.forceCloseCount = forceCloseCount;
    }
}
