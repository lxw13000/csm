package com.tsd.csm.modules.tenant.controller;

import com.tsd.csm.core.audit.AuditLog;
import com.tsd.csm.core.common.enums.AccountType;
import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.RequireRole;
import com.tsd.csm.modules.tenant.domain.Tenant;
import com.tsd.csm.modules.tenant.domain.dto.TenantQuery;
import com.tsd.csm.modules.tenant.domain.dto.TenantSaveDTO;
import com.tsd.csm.modules.tenant.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户接入管理（仅平台超级管理员）。
 */
@RestController
@RequestMapping("/api/admin/tenant")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * 分页查询租户。
     * @param query 查询条件
     * @return 租户分页结果
     */
    @GetMapping("/page")
    @RequireRole(AccountType.PLATFORM_SUPER)
    public R<PageResult<Tenant>> page(TenantQuery query) {
        return R.ok(tenantService.pageTenants(query));
    }

    /**
     * 租户详情。
     * @param id 租户 id
     * @return 租户
     */
    @GetMapping("/{id}")
    @RequireRole(AccountType.PLATFORM_SUPER)
    public R<Tenant> get(@PathVariable Long id) {
        return R.ok(tenantService.getById(id));
    }

    /**
     * 新增租户接入（并预置默认配置）。
     * @param dto 租户信息
     * @return 新增的租户
     */
    @PostMapping
    @RequireRole(AccountType.PLATFORM_SUPER)
    @AuditLog(module = "tenant", action = "create", targetType = "tenant")
    public R<Tenant> create(@RequestBody @Valid TenantSaveDTO dto) {
        return R.ok(tenantService.createTenant(dto));
    }

    /**
     * 编辑租户接入。
     * @param id 租户 id
     * @param dto 租户信息
     * @return 更新后的租户
     */
    @PutMapping("/{id}")
    @RequireRole(AccountType.PLATFORM_SUPER)
    public R<Tenant> update(@PathVariable Long id, @RequestBody @Valid TenantSaveDTO dto) {
        return R.ok(tenantService.updateTenant(id, dto));
    }

    /**
     * 启用/停用租户接入。
     * @param id 租户 id
     * @param status 状态：1 启用 / 0 停用
     */
    @PutMapping("/{id}/status")
    @RequireRole(AccountType.PLATFORM_SUPER)
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        tenantService.changeStatus(id, status);
        return R.ok();
    }
}
