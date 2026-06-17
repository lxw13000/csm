package com.tsd.csm.modules.account.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.modules.account.domain.Account;
import com.tsd.csm.modules.account.domain.dto.AccountQuery;
import com.tsd.csm.modules.account.domain.dto.AccountSaveDTO;
import com.tsd.csm.modules.account.domain.vo.AccountVO;

import java.util.List;

/**
 * 内部账号服务（PC 后台 + 客服 H5 账号）。
 */
public interface AccountService extends IService<Account> {

    /** 当前租户上下文内按用户名查账号（供登录使用）。 */
    Account findByUsername(String username);

    AccountVO createAccount(AccountSaveDTO dto);

    AccountVO updateAccount(Long id, AccountSaveDTO dto);

    void resetPassword(Long id, String newPassword);

    void changeStatus(Long id, Integer status);

    PageResult<AccountVO> pageAccounts(AccountQuery query);

    /** 本租户客服账号列表（account_type=3），用于转接选择等。 */
    List<AccountVO> listAgents();

    AccountVO toVO(Account account);
}
