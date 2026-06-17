package com.tsd.csm.modules.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsd.csm.core.common.exception.BizException;
import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.core.tenant.TenantContext;
import com.tsd.csm.modules.config.domain.TenantConfig;
import com.tsd.csm.modules.config.mapper.TenantConfigMapper;
import com.tsd.csm.modules.tenant.domain.Tenant;
import com.tsd.csm.modules.tenant.domain.dto.TenantQuery;
import com.tsd.csm.modules.tenant.domain.dto.TenantSaveDTO;
import com.tsd.csm.modules.tenant.mapper.TenantMapper;
import com.tsd.csm.modules.tenant.service.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 租户接入服务实现：租户增删改查与新租户默认配置预置。
 */
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    private final TenantConfigMapper tenantConfigMapper;

    public TenantServiceImpl(TenantConfigMapper tenantConfigMapper) {
        this.tenantConfigMapper = tenantConfigMapper;
    }

    @Override
    public Tenant getByAppId(String appId) {
        return lambdaQuery().eq(Tenant::getAppId, appId).one();
    }

    @Override
    public Tenant getEnabledOrThrow(String appId) {
        Tenant tenant = getByAppId(appId);
        if (tenant == null) {
            throw new BizException("租户不存在：" + appId);
        }
        if (tenant.getStatus() == null || tenant.getStatus() != 1) {
            throw new BizException("租户已停用：" + appId);
        }
        return tenant;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tenant createTenant(TenantSaveDTO dto) {
        if (getByAppId(dto.getAppId()) != null) {
            throw new BizException("app_id 已存在：" + dto.getAppId());
        }
        Tenant tenant = new Tenant();
        apply(tenant, dto);
        save(tenant);

        // 为新租户预置默认配置（在目标租户上下文中插入，使拦截器写入正确 app_id）
        TenantContext.runWithAppId(dto.getAppId(), () -> {
            TenantConfig config = new TenantConfig();
            config.setMaxConcurrent(0);
            config.setAutoCloseMinutes(15);
            config.setNotifySound(1);
            tenantConfigMapper.insert(config);
        });
        return tenant;
    }

    @Override
    public Tenant updateTenant(Long id, TenantSaveDTO dto) {
        Tenant tenant = getById(id);
        if (tenant == null) {
            throw new BizException("租户不存在");
        }
        if (!tenant.getAppId().equals(dto.getAppId()) && getByAppId(dto.getAppId()) != null) {
            throw new BizException("app_id 已存在：" + dto.getAppId());
        }
        apply(tenant, dto);
        updateById(tenant);
        return tenant;
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        Tenant tenant = getById(id);
        if (tenant == null) {
            throw new BizException("租户不存在");
        }
        tenant.setStatus(status);
        updateById(tenant);
    }

    @Override
    public PageResult<Tenant> pageTenants(TenantQuery query) {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Tenant::getName, query.getKeyword())
                    .or().like(Tenant::getAppId, query.getKeyword()));
        }
        if (query.getStatus() != null) {
            wrapper.eq(Tenant::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Tenant::getId);
        Page<Tenant> page = page(Page.of(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.of(page);
    }

    /** 将入参拷贝到租户实体（status 缺省为启用）。 */
    private void apply(Tenant tenant, TenantSaveDTO dto) {
        tenant.setAppId(dto.getAppId());
        tenant.setAppSecret(dto.getAppSecret());
        tenant.setName(dto.getName());
        tenant.setIdentityApi(dto.getIdentityApi());
        tenant.setUserInfoApi(dto.getUserInfoApi());
        tenant.setIpWhitelist(dto.getIpWhitelist());
        tenant.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        tenant.setRemark(dto.getRemark());
    }
}
