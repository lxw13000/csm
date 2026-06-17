package com.tsd.csm.modules.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.account.domain.AccountRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账号-角色关联 Mapper。对应 csm_account_role。
 */
@Mapper
public interface AccountRoleMapper extends BaseMapper<AccountRole> {
}
