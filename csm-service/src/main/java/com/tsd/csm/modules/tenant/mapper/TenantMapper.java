package com.tsd.csm.modules.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.tenant.domain.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户 Mapper。对应 csm_tenant。
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
