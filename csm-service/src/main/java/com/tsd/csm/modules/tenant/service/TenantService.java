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

    /**
     * 新增租户接入（app_id 查重并预置默认配置）。
     * @param dto 租户信息
     * @return 新增的租户
     */
    Tenant createTenant(TenantSaveDTO dto);

    /**
     * 编辑租户接入。
     * @param id 租户 id
     * @param dto 租户信息
     * @return 更新后的租户
     */
    Tenant updateTenant(Long id, TenantSaveDTO dto);

    /**
     * 启用/停用租户。
     * @param id 租户 id
     * @param status 状态：1 启用 / 0 停用
     */
    void changeStatus(Long id, Integer status);

    /**
     * 分页查询租户。
     * @param query 查询条件
     * @return 租户分页结果
     */
    PageResult<Tenant> pageTenants(TenantQuery query);
}
