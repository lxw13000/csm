package com.tsd.csm.core.startup;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tsd.csm.core.common.constant.CsmConst;
import com.tsd.csm.core.common.enums.AccountType;
import com.tsd.csm.core.config.CsmProperties;
import com.tsd.csm.core.tenant.TenantContext;
import com.tsd.csm.modules.account.domain.Account;
import com.tsd.csm.modules.account.domain.AccountRole;
import com.tsd.csm.modules.account.mapper.AccountMapper;
import com.tsd.csm.modules.account.mapper.AccountRoleMapper;
import com.tsd.csm.modules.agent.domain.AgentStatus;
import com.tsd.csm.modules.agent.mapper.AgentStatusMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时幂等初始化内置账号。
 *
 * <p>菜单字典、平台/租户角色、演示租户与租户配置已由 {@code data.sql} 建立；
 * 此处仅补带 bcrypt 密码的账号（无法在 SQL 中正确生成）与客服在线状态行。
 *
 * <ul>
 *   <li>平台超管：{@code _platform_ / admin}，绑定平台角色 id=1。</li>
 *   <li>演示租户 {@code biz_demo}：管理员 {@code admin}（角色 id=2）、客服 {@code agent1/agent2}。</li>
 * </ul>
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String DEMO_APP_ID = "biz_demo";
    private static final long PLATFORM_ROLE_ID = 1L;
    private static final long DEMO_ADMIN_ROLE_ID = 2L;
    private static final String AGENT_DEFAULT_PASSWORD = "agent123";

    private final AccountMapper accountMapper;
    private final AccountRoleMapper accountRoleMapper;
    private final AgentStatusMapper agentStatusMapper;
    private final PasswordEncoder passwordEncoder;
    private final CsmProperties properties;

    public DataInitializer(AccountMapper accountMapper, AccountRoleMapper accountRoleMapper,
                           AgentStatusMapper agentStatusMapper, PasswordEncoder passwordEncoder,
                           CsmProperties properties) {
        this.accountMapper = accountMapper;
        this.accountRoleMapper = accountRoleMapper;
        this.agentStatusMapper = agentStatusMapper;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String adminPassword = properties.getInit().getDefaultPassword();

        TenantContext.runWithAppId(CsmConst.PLATFORM_APP_ID, () -> {
            Long platformAdminId = ensureAccount("admin", adminPassword,
                    AccountType.PLATFORM_SUPER.getCode(), "平台超级管理员");
            bindRole(platformAdminId, PLATFORM_ROLE_ID);
        });

        TenantContext.runWithAppId(DEMO_APP_ID, () -> {
            Long demoAdminId = ensureAccount("admin", adminPassword,
                    AccountType.TENANT_ADMIN.getCode(), "演示租户管理员");
            bindRole(demoAdminId, DEMO_ADMIN_ROLE_ID);
            ensureAgent("agent1", "客服一号");
            ensureAgent("agent2", "客服二号");
        });

        log.info("内置账号初始化完成（平台超管 _platform_/admin，演示租户 biz_demo/admin、agent1、agent2）");
    }

    /** 幂等创建账号，返回账号 id（已存在则返回既有 id）。要求调用方已绑定租户上下文。 */
    private Long ensureAccount(String username, String rawPassword, int accountType, String realName) {
        Account existing = accountMapper.selectOne(
                new LambdaQueryWrapper<Account>().eq(Account::getUsername, username));
        if (existing != null) {
            return existing.getId();
        }
        Account account = new Account();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setRealName(realName);
        account.setAccountType(accountType);
        account.setStatus(1);
        accountMapper.insert(account);
        return account.getId();
    }

    private void ensureAgent(String username, String realName) {
        Long agentId = ensureAccount(username, AGENT_DEFAULT_PASSWORD, AccountType.AGENT.getCode(), realName);
        ensureAgentStatus(agentId);
    }

    private void ensureAgentStatus(Long accountId) {
        Long count = agentStatusMapper.selectCount(
                new LambdaQueryWrapper<AgentStatus>().eq(AgentStatus::getAccountId, accountId));
        if (count != null && count > 0) {
            return;
        }
        AgentStatus status = new AgentStatus();
        status.setAccountId(accountId);
        status.setOnlineStatus(0);
        status.setCurrentLoad(0);
        agentStatusMapper.insert(status);
    }

    private void bindRole(Long accountId, Long roleId) {
        Long count = accountRoleMapper.selectCount(new LambdaQueryWrapper<AccountRole>()
                .eq(AccountRole::getAccountId, accountId)
                .eq(AccountRole::getRoleId, roleId));
        if (count != null && count > 0) {
            return;
        }
        AccountRole accountRole = new AccountRole();
        accountRole.setAccountId(accountId);
        accountRole.setRoleId(roleId);
        accountRoleMapper.insert(accountRole);
    }
}
