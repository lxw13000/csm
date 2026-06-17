package com.tsd.csm.modules.account.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 账号新增/编辑入参。新增时 password 必填；编辑时为空表示不修改密码。
 */
public class AccountSaveDTO {

    /** 用户名。 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码：新增必填；编辑时留空表示不修改。 */
    private String password;

    /** 姓名。 */
    private String realName;

    /** 账号类型：1 平台超管 / 2 租户管理员 / 3 客服。 */
    @NotNull(message = "账号类型不能为空")
    private Integer accountType;

    /** 状态：1 启用 / 0 禁用。 */
    private Integer status;

    /** 绑定的角色 id 列表。 */
    private List<Long> roleIds;

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

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
