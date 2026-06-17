package com.tsd.csm.modules.account.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.TimedEntity;

/**
 * 内部账号（PC 后台 + 客服 H5）。对应 csm_account。
 */
@TableName("csm_account")
public class Account extends TimedEntity {

    /** 所属租户；平台超管用保留值 _platform_。 */
    private String appId;
    /** 登录账号。 */
    private String username;
    /** 密码哈希（bcrypt）。 */
    private String passwordHash;
    /** 姓名。 */
    private String realName;
    /** 账号类型：1 平台超管 / 2 租户管理员 / 3 客服。 */
    private Integer accountType;
    /** 状态：1 启用 / 0 禁用。 */
    private Integer status;

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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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
}
