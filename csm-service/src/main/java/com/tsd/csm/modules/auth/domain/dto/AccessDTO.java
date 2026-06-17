package com.tsd.csm.modules.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * H5 用户接入入参：业务 App 传入 app_id + 一次性 token（呼应 xuqiu.md 2.3）。
 */
public class AccessDTO {

    /** 租户标识（业务系统 app_id）。 */
    @NotBlank(message = "appId 不能为空")
    private String appId;

    /** 业务 App 传入的一次性临时 token，用于换取 user_id。 */
    @NotBlank(message = "token 不能为空")
    private String token;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
