package com.tsd.csm.modules.qa.domain.dto;

import com.tsd.csm.core.common.query.PageQuery;

/**
 * QA 分页查询入参。
 */
public class QaQuery extends PageQuery {

    /** 问题模糊关键字。 */
    private String keyword;
    /** 状态筛选：1 启用 / 0 停用。 */
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
