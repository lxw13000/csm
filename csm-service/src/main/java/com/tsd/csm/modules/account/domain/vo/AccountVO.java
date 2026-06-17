package com.tsd.csm.modules.account.domain.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账号展示 VO（不含密码哈希）。
 */
public class AccountVO {

    /** 账号 id。 */
    private Long id;
    /** 所属租户。 */
    private String appId;
    /** 登录账号。 */
    private String username;
    /** 姓名。 */
    private String realName;
    /** 账号类型：1 平台超管 / 2 租户管理员 / 3 客服。 */
    private Integer accountType;
    /** 状态：1 启用 / 0 禁用。 */
    private Integer status;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 已绑定的角色 id 列表。 */
    private List<Long> roleIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
