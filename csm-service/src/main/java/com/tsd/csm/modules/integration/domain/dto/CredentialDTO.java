package com.tsd.csm.modules.integration.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 颁发通信凭证入参：业务系统用 app_id + app_secret 鉴权，传入自身 user_id 与可选昵称/头像（呼应 xuqiu.md 2.4）。
 */
public class CredentialDTO {

    /** 租户标识（业务系统 app_id）。 */
    @NotBlank(message = "appId 不能为空")
    private String appId;

    /** 换取通信凭证的鉴权密钥（app_secret）。 */
    @NotBlank(message = "appSecret 不能为空")
    private String appSecret;

    /** 业务系统用户 id（必填）。 */
    @NotBlank(message = "userId 不能为空")
    private String userId;

    /** 用户昵称（选填，用于缓存展示）。 */
    private String nickname;

    /** 用户头像 URL（选填，用于缓存展示）。 */
    private String avatar;

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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
