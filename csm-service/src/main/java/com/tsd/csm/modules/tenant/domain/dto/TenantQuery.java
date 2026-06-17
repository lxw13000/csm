package com.tsd.csm.modules.tenant.domain.dto;

import com.tsd.csm.core.common.query.PageQuery;

/**
 * 租户分页查询入参。
 */
public class TenantQuery extends PageQuery {

    /** 租户名称/appId 模糊关键字。 */
    private String keyword;
    /** 接入状态筛选：1 启用 / 0 停用。 */
    private Integer status;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
