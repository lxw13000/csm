package com.tsd.csm.modules.agent.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 客服在线状态/负载（派单依据）。对应 csm_agent_status。
 */
@TableName("csm_agent_status")
public class AgentStatus extends BaseEntity {

    /** 所属租户。 */
    private String appId;
    /** 客服账号 id（csm_account.id）。 */
    private Long accountId;
    /** 在线状态：0 离线 / 1 在线。 */
    private Integer onlineStatus;
    /** 当前处理中工单数（派单负载依据）。 */
    private Integer currentLoad;
    /** 最近上线时间。 */
    private LocalDateTime lastOnlineAt;

    /** 更新时间。 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(Integer onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public Integer getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(Integer currentLoad) {
        this.currentLoad = currentLoad;
    }

    public LocalDateTime getLastOnlineAt() {
        return lastOnlineAt;
    }

    public void setLastOnlineAt(LocalDateTime lastOnlineAt) {
        this.lastOnlineAt = lastOnlineAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
