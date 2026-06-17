package com.tsd.csm.modules.account.domain.dto;

import com.tsd.csm.core.common.query.PageQuery;

/**
 * 账号分页查询入参。
 */
public class AccountQuery extends PageQuery {

    /** 用户名/姓名模糊关键字。 */
    private String keyword;
    /** 账号类型筛选：1 平台超管 / 2 租户管理员 / 3 客服。 */
    private Integer accountType;
    /** 状态筛选：1 启用 / 0 禁用。 */
    private Integer status;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
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
