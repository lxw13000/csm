package com.tsd.csm.modules.account.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.BaseEntity;

/**
 * 账号-角色关联。对应 csm_account_role。
 */
@TableName("csm_account_role")
public class AccountRole extends BaseEntity {

    /** 所属租户（隔离冗余）。 */
    private String appId;
    /** 账号 id。 */
    private Long accountId;
    /** 角色 id。 */
    private Long roleId;

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

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
