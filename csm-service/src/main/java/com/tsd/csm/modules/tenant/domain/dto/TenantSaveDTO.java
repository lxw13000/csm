package com.tsd.csm.modules.tenant.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 租户接入新增/编辑入参。
 */
public class TenantSaveDTO {

    /** 租户标识（对接业务系统唯一标识）。 */
    @NotBlank(message = "app_id 不能为空")
    private String appId;

    /** 换取通信凭证的鉴权密钥。 */
    @NotBlank(message = "app_secret 不能为空")
    private String appSecret;

    /** 租户/业务系统名称。 */
    @NotBlank(message = "租户名称不能为空")
    private String name;

    /** 颁发给业务系统的通信凭证有效期（分钟），为空取默认 120。 */
    private Integer credentialExpireMinutes;

    /** IP 白名单，逗号分隔，可空。 */
    private String ipWhitelist;
    /** 接入状态：1 启用 / 0 停用，默认启用。 */
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
