package com.tsd.csm.modules.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsd.csm.core.common.enums.AccountType;
import com.tsd.csm.core.common.exception.BizException;
import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.core.common.result.ResultCode;
import com.tsd.csm.core.security.LoginUser;
import com.tsd.csm.core.security.UserContext;
import com.tsd.csm.modules.account.domain.Account;
import com.tsd.csm.modules.account.domain.AccountRole;
import com.tsd.csm.modules.account.domain.dto.AccountQuery;
import com.tsd.csm.modules.account.domain.dto.AccountSaveDTO;
import com.tsd.csm.modules.account.domain.vo.AccountVO;
import com.tsd.csm.modules.account.mapper.AccountMapper;
import com.tsd.csm.modules.account.mapper.AccountRoleMapper;
import com.tsd.csm.modules.account.service.AccountService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 内部账号服务实现：账号增删改查、密码与状态维护、角色绑定。
 */
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    private final AccountRoleMapper accountRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AccountRoleMapper accountRoleMapper, PasswordEncoder passwordEncoder) {
        this.accountRoleMapper = accountRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Account findByUsername(String username) {
        return lambdaQuery().eq(Account::getUsername, username).one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountVO createAccount(AccountSaveDTO dto) {
        guardAccountType(dto.getAccountType());
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BizException("新增账号必须设置密码");
        }
        if (findByUsername(dto.getUsername()) != null) {
            throw new BizException("用户名已存在：" + dto.getUsername());
        }
        Account account = new Account();
        account.setUsername(dto.getUsername());
        account.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        account.setRealName(dto.getRealName());
        account.setAccountType(dto.getAccountType());
        account.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        save(account);
        bindRoles(account.getId(), dto.getRoleIds());
        return toVO(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountVO updateAccount(Long id, AccountSaveDTO dto) {
        Account account = getOwned(id);
        if (dto.getAccountType() != null && !dto.getAccountType().equals(account.getAccountType())) {
            guardAccountType(dto.getAccountType());
            account.setAccountType(dto.getAccountType());
        }
        account.setRealName(dto.getRealName());
        if (dto.getStatus() != null) {
            account.setStatus(dto.getStatus());
        }
        if (StringUtils.hasText(dto.getPassword())) {
            account.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        updateById(account);
        if (dto.getRoleIds() != null) {
            accountRoleMapper.delete(new LambdaQueryWrapper<AccountRole>().eq(AccountRole::getAccountId, id));
            bindRoles(id, dto.getRoleIds());
        }
        return toVO(account);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        Account account = getOwned(id);
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        updateById(account);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        Account account = getOwned(id);
        account.setStatus(status);
        updateById(account);
    }

    @Override
    public PageResult<AccountVO> pageAccounts(AccountQuery query) {
        Page<Account> page = lambdaQuery()
                .eq(query.getAccountType() != null, Account::getAccountType, query.getAccountType())
                .eq(query.getStatus() != null, Account::getStatus, query.getStatus())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(Account::getUsername, query.getKeyword())
                        .or().like(Account::getRealName, query.getKeyword()))
                .orderByDesc(Account::getId)
                .page(Page.of(query.getCurrent(), query.getSize()));
        return PageResult.of(page, this::toVO);
    }

    @Override
    public List<AccountVO> listAgents() {
        return lambdaQuery()
                .eq(Account::getAccountType, AccountType.AGENT.getCode())
                .eq(Account::getStatus, 1)
                .list().stream().map(this::toVO).toList();
    }

    @Override
    public AccountVO toVO(Account account) {
        AccountVO vo = new AccountVO();
        vo.setId(account.getId());
        vo.setAppId(account.getAppId());
        vo.setUsername(account.getUsername());
        vo.setRealName(account.getRealName());
        vo.setAccountType(account.getAccountType());
        vo.setStatus(account.getStatus());
        vo.setCreatedAt(account.getCreatedAt());
        List<Long> roleIds = accountRoleMapper
                .selectList(new LambdaQueryWrapper<AccountRole>().eq(AccountRole::getAccountId, account.getId()))
                .stream().map(AccountRole::getRoleId).toList();
        vo.setRoleIds(roleIds);
        return vo;
    }

    /** 获取本租户内的账号，越权抛 403。 */
    private Account getOwned(Long id) {
        Account account = getById(id);
        if (account == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return account;
    }

    /** 批量绑定角色到账号。 */
    private void bindRoles(Long accountId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            AccountRole ar = new AccountRole();
            ar.setAccountId(accountId);
            ar.setRoleId(roleId);
            accountRoleMapper.insert(ar);
        }
    }

    /** 仅平台超管可创建平台超管账号。 */
    private void guardAccountType(Integer accountType) {
        if (accountType != null && accountType == AccountType.PLATFORM_SUPER.getCode()) {
            LoginUser current = UserContext.get();
            if (current == null || !current.isPlatformSuper()) {
                throw new BizException(ResultCode.FORBIDDEN.getCode(), "仅平台超管可创建平台超管账号");
            }
        }
    }
}
