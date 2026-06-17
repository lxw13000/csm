package com.tsd.csm.modules.ticket.domain.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 工单转接入参。
 */
public class TransferDTO {

    /** 转入客服账号 id。 */
    @NotNull(message = "转入客服不能为空")
    private Long toAgentId;

    /** 转接原因。 */
    private String reason;

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
}
