package com.tsd.csm.modules.tenant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.TimedEntity;

/**
 * 业务系统(租户)接入。对应 csm_tenant。
 * 注意：app_id 为业务主键，由平台超管显式维护，不参与行级租户过滤（见 CsmTenantLineHandler）。
 */
@TableName("csm_tenant")
public class Tenant extends TimedEntity {

    /** 租户标识，对接业务系统唯一标识。 */
    private String appId;
    /** 换取通信凭证的鉴权密钥。 */
    private String appSecret;
    /** 租户/业务系统名称。 */
    private String name;
    /** 颁发给业务系统的通信凭证有效期（分钟）。 */
    private Integer credentialExpireMinutes;
    /** IP 白名单，逗号分隔，可空。 */
    private String ipWhitelist;
    /** 接入状态：1 启用 / 0 停用。 */
    private Integer status;
    /** 备注。 */
    private String remark;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCredentialExpireMinutes() {
        return credentialExpireMinutes;
    }

    public void setCredentialExpireMinutes(Integer credentialExpireMinutes) {
        this.credentialExpireMinutes = credentialExpireMinutes;
    }

    public String getIpWhitelist() {
        return ipWhitelist;
    }

    public void setIpWhitelist(String ipWhitelist) {
        this.ipWhitelist = ipWhitelist;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
