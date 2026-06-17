package com.tsd.csm.core.common.enums;

/**
 * 工单状态。对应 csm_ticket.status。
 */
public enum TicketStatus {

    /** 智能问答（QA 阶段，尚未转人工）。 */
    QA(1, "智能问答"),
    /** 人工转接中（已进入队列，等待派单）。 */
    TRANSFERRING(2, "人工转接中"),
    /** 处理中（已派单给客服）。 */
    PROCESSING(3, "处理中"),
    /** 已完结。 */
    CLOSED(4, "已完结");

    /** 编码值，对应数据库列。 */
    private final int code;
    /** 中文描述。 */
    private final String desc;

    TicketStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
