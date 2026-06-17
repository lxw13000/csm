package com.tsd.csm.modules.stats.domain.vo;

/**
 * 工单统计 VO（本租户，按日期范围）。
 */
public class TicketStatsVO {

    /** 工单总量。 */
    private long total;
    /** 智能问答阶段工单数。 */
    private long qa;
    /** 人工转接中工单数。 */
    private long transferring;
    /** 处理中工单数。 */
    private long processing;
    /** 已完结工单数。 */
    private long closed;
    /** 完结率（closed / total）。 */
    private double closeRate;
    /** 平均处理时长（秒，完结工单 closedAt − createdAt 均值）。 */
    private long avgHandleSeconds;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getQa() {
        return qa;
    }

    public void setQa(long qa) {
        this.qa = qa;
    }

    public long getTransferring() {
        return transferring;
    }

    public void setTransferring(long transferring) {
        this.transferring = transferring;
    }

    public long getProcessing() {
        return processing;
    }

    public void setProcessing(long processing) {
        this.processing = processing;
    }

    public long getClosed() {
        return closed;
    }

    public void setClosed(long closed) {
        this.closed = closed;
    }

    public double getCloseRate() {
        return closeRate;
    }

    public void setCloseRate(double closeRate) {
        this.closeRate = closeRate;
    }

    public long getAvgHandleSeconds() {
        return avgHandleSeconds;
    }

    public void setAvgHandleSeconds(long avgHandleSeconds) {
        this.avgHandleSeconds = avgHandleSeconds;
    }
}
