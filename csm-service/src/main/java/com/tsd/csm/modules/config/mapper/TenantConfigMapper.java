package com.tsd.csm.modules.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.config.domain.TenantConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantConfigMapper extends BaseMapper<TenantConfig> {
}
