package com.tsd.csm.core.security;

import java.io.Serializable;

/**
 * 登录主体。可表示两类身份：
 * <ul>
 *   <li>内部账号（PC/客服）：{@code accountId} + {@code accountType} 有值，{@code userId} 为空。</li>
 *   <li>C 端 H5 用户：{@code userId} 有值（业务系统 user_id），{@code accountId} 为空。</li>
 * </ul>
 * 两者均绑定 {@code appId}。
 */
public class LoginUser implements Serializable {

    /** 所属租户（隔离键）。 */
    private String appId;
    /** 内部账号 id（C 端用户为 null）。 */
    private Long accountId;
    /** 账号类型：1 平台超管 / 2 租户管理员 / 3 客服（C 端用户为 null）。 */
    private Integer accountType;
    /** 登录账号（内部账号）。 */
    private String username;
    /** 姓名（内部账号）。 */
    private String realName;
    /** 业务系统用户 id（C 端用户；内部账号为 null）。 */
    private String userId;

    /** 是否为 C 端用户身份。 */
    public boolean isCustomer() {
        return userId != null && accountId == null;
    }

    /** 是否为平台超级管理员。 */
    public boolean isPlatformSuper() {
        return accountType != null && accountType == 1;
    }

    /** 是否为租户管理员。 */
    public boolean isTenantAdmin() {
        return accountType != null && accountType == 2;
    }

    /** 是否为客服。 */
    public boolean isAgent() {
        return accountType != null && accountType == 3;
    }

    /** 构造内部账号（PC/客服）登录主体。 */
    public static LoginUser ofAccount(String appId, Long accountId, Integer accountType,
                                      String username, String realName) {
        LoginUser u = new LoginUser();
        u.appId = appId;
        u.accountId = accountId;
        u.accountType = accountType;
        u.username = username;
        u.realName = realName;
        return u;
    }

    /** 构造 C 端用户登录主体。 */
    public static LoginUser ofCustomer(String appId, String userId) {
        LoginUser u = new LoginUser();
        u.appId = appId;
        u.userId = userId;
        return u;
    }

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

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
