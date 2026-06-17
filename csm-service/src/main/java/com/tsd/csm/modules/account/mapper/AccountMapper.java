package com.tsd.csm.modules.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.account.domain.Account;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账号 Mapper。对应 csm_account。
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
