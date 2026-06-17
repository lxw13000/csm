package com.tsd.csm.modules.tenant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.modules.tenant.domain.Tenant;
import com.tsd.csm.modules.tenant.domain.dto.TenantQuery;
import com.tsd.csm.modules.tenant.domain.dto.TenantSaveDTO;

/**
 * 租户接入服务（平台超管维护）。
 */
public interface TenantService extends IService<Tenant> {

    /** 按 app_id 获取租户（含停用），不存在返回 null。 */
    Tenant getByAppId(String appId);

    /** 按 app_id 获取启用中的租户，不存在或停用抛异常。 */
    Tenant getEnabledOrThrow(String appId);

    Tenant createTenant(TenantSaveDTO dto);

    Tenant updateTenant(Long id, TenantSaveDTO dto);

    void changeStatus(Long id, Integer status);

    PageResult<Tenant> pageTenants(TenantQuery query);
}
