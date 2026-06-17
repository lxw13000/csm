package com.tsd.csm.modules.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.account.domain.RoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色-菜单关联 Mapper。对应 csm_role_menu。
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {
}
