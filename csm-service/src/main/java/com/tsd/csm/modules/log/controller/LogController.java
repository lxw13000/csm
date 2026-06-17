package com.tsd.csm.modules.log.controller;

import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.RequiresPermission;
import com.tsd.csm.modules.log.domain.OperationLog;
import com.tsd.csm.modules.log.domain.dto.OperationLogQuery;
import com.tsd.csm.modules.log.service.OperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作审计日志查询（本租户；平台超管可经 X-App-Id 切换查看各租户）。
 */
@RestController
@RequestMapping("/api/admin/log")
public class LogController {

    private final OperationLogService operationLogService;

    public LogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping("/page")
    @RequiresPermission("log:list")
    public R<PageResult<OperationLog>> page(OperationLogQuery query) {
        return R.ok(operationLogService.page(query));
    }
}
