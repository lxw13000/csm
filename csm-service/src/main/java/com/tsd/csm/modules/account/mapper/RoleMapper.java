package com.tsd.csm.modules.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.account.domain.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper。对应 csm_role。
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
