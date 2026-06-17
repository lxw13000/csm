package com.tsd.csm.modules.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.account.domain.Menu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜单 Mapper。对应 csm_menu。
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
}
