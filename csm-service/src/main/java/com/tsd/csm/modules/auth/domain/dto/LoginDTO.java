package com.tsd.csm.modules.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 内部账号（PC 后台 / 客服 H5）登录入参。平台超管 appId 传保留值 {@code _platform_}。
 */
public class LoginDTO {

    /** 登录租户；平台超管用保留值 _platform_。 */
    @NotBlank(message = "appId 不能为空")
    private String appId;

    /** 登录用户名。 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 登录密码（明文，服务端比对 bcrypt 哈希）。 */
    @NotBlank(message = "密码不能为空")
    private String password;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
