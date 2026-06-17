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

    /**
     * 新增账号（含角色绑定）。
     * @param dto 账号信息
     * @return 新增账号 VO
     */
    AccountVO createAccount(AccountSaveDTO dto);

    /**
     * 编辑账号（密码留空表示不修改）。
     * @param id 账号 id
     * @param dto 账号信息
     * @return 更新后账号 VO
     */
    AccountVO updateAccount(Long id, AccountSaveDTO dto);

    /**
     * 重置账号密码。
     * @param id 账号 id
     * @param newPassword 新密码
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 启用/禁用账号。
     * @param id 账号 id
     * @param status 状态：1 启用 / 0 禁用
     */
    void changeStatus(Long id, Integer status);

    /**
     * 分页查询本租户账号。
     * @param query 查询条件
     * @return 账号分页结果
     */
    PageResult<AccountVO> pageAccounts(AccountQuery query);

    /** 本租户客服账号列表（account_type=3），用于转接选择等。 */
    List<AccountVO> listAgents();

    /**
     * 账号实体转展示 VO（含角色 id，不含密码哈希）。
     * @param account 账号实体
     * @return 账号 VO
     */
    AccountVO toVO(Account account);
}
