package com.tsd.csm.modules.config.controller;

import com.tsd.csm.core.audit.AuditLog;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.RequiresPermission;
import com.tsd.csm.modules.config.domain.TenantConfig;
import com.tsd.csm.modules.config.domain.dto.TenantConfigUpdateDTO;
import com.tsd.csm.modules.config.service.TenantConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客服接入配置（本租户）：单客服最大接入阈值、自动完结时长、提醒开关。
 */
@RestController
@RequestMapping("/api/admin/config")
public class ConfigController {

    private final TenantConfigService tenantConfigService;

    public ConfigController(TenantConfigService tenantConfigService) {
        this.tenantConfigService = tenantConfigService;
    }

    @GetMapping
    @RequiresPermission("config:view")
    public R<TenantConfig> current() {
        return R.ok(tenantConfigService.getCurrent());
    }

    @PutMapping
    @RequiresPermission("config:view")
    @AuditLog(module = "config", action = "update", targetType = "tenant_config")
    public R<TenantConfig> update(@RequestBody @Valid TenantConfigUpdateDTO dto) {
        return R.ok(tenantConfigService.update(dto));
    }
}
