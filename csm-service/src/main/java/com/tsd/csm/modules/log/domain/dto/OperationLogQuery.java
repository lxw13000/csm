package com.tsd.csm.modules.log.domain.dto;

import com.tsd.csm.core.common.query.PageQuery;

/**
 * 操作日志分页查询入参。
 */
public class OperationLogQuery extends PageQuery {

    /** 按操作模块筛选，如 ticket/qa/account。 */
    private String module;
    /** 按操作动作筛选，如 create/update/close。 */
    private String action;
    /** 按操作人账号 id 筛选。 */
    private Long operatorId;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }
}
