package com.tsd.csm.modules.log.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 后台操作审计日志。对应 csm_operation_log。
 */
@TableName("csm_operation_log")
public class OperationLog extends BaseEntity {

    /** 操作所属租户；平台级操作为 _platform_。 */
    private String appId;
    /** 操作人账号 id（csm_account.id）。 */
    private Long operatorId;
    /** 操作人姓名（快照）。 */
    private String operatorName;
    /** 操作人类型：1 平台超管 / 2 租户管理员 / 3 客服。 */
    private Integer operatorType;
    /** 操作模块，如 ticket/qa/account/tenant/config/auth。 */
    private String module;
    /** 操作动作，如 create/update/delete/close/transfer/login。 */
    private String action;
    /** 目标对象类型。 */
    private String targetType;
    /** 目标对象 id。 */
    private String targetId;
    /** 操作详情（JSON 文本，请求参数/变更前后快照）。 */
    private String detail;
    /** 操作来源 IP（兼容 IPv6）。 */
    private String clientIp;
    /** 客户端 User-Agent。 */
    private String userAgent;

    /** 操作时间（毫秒精度）。 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Integer getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(Integer operatorType) {
        this.operatorType = operatorType;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
