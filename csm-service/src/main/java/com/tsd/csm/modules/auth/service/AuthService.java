package com.tsd.csm.modules.auth.service;

import com.tsd.csm.core.common.exception.BizException;
import com.tsd.csm.core.common.result.ResultCode;
import com.tsd.csm.core.security.JwtUtil;
import com.tsd.csm.core.security.LoginUser;
import com.tsd.csm.core.security.PermissionLoader;
import com.tsd.csm.core.security.UserContext;
import com.tsd.csm.core.tenant.TenantContext;
import com.tsd.csm.modules.account.domain.Account;
import com.tsd.csm.modules.account.service.AccountService;
import com.tsd.csm.modules.account.service.MenuService;
import com.tsd.csm.modules.auth.domain.dto.AccessDTO;
import com.tsd.csm.modules.auth.domain.dto.LoginDTO;
import com.tsd.csm.modules.auth.domain.vo.AccessVO;
import com.tsd.csm.modules.auth.domain.vo.LoginVO;
import com.tsd.csm.modules.customer.domain.Customer;
import com.tsd.csm.modules.customer.service.CustomerService;
import com.tsd.csm.modules.integration.client.BizSystemClient;
import com.tsd.csm.modules.tenant.domain.Tenant;
import com.tsd.csm.modules.tenant.service.TenantService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 认证服务：内部账号登录、当前用户信息、H5 用户接入（token 换取会话凭证）。
 */
@Service
public class AuthService {

    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PermissionLoader permissionLoader;
    private final MenuService menuService;
    private final TenantService tenantService;
    private final BizSystemClient bizSystemClient;
    private final CustomerService customerService;

    public AuthService(AccountService accountService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       PermissionLoader permissionLoader, MenuService menuService, TenantService tenantService,
                       BizSystemClient bizSystemClient, CustomerService customerService) {
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.permissionLoader = permissionLoader;
        this.menuService = menuService;
        this.tenantService = tenantService;
        this.bizSystemClient = bizSystemClient;
        this.customerService = customerService;
    }

    /**
     * 内部账号登录。{@code allowedTypes} 限定可从该入口登录的账号类型（PC 端 vs 客服端）。
     * 登录路径不经 AuthInterceptor，故按 dto.appId 显式绑定租户上下文后再查账号，保证隔离。
     */
    public LoginVO login(LoginDTO dto, Set<Integer> allowedTypes) {
        Account account = TenantContext.callWithAppId(dto.getAppId(),
                () -> accountService.findByUsername(dto.getUsername()));
        if (account == null || !passwordEncoder.matches(dto.getPassword(), account.getPasswordHash())) {
            throw new BizException("用户名或密码错误");
        }
        if (account.getStatus() == null || account.getStatus() != 1) {
            throw new BizException("账号已被禁用");
        }
        if (!allowedTypes.contains(account.getAccountType())) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "该账号类型不允许从此入口登录");
        }
        LoginUser user = LoginUser.ofAccount(account.getAppId(), account.getId(), account.getAccountType(),
                account.getUsername(), account.getRealName());
        String token = jwtUtil.generateAccountToken(user);
        LoginVO vo = buildUserInfo(user);
        vo.setToken(token);
        return vo;
    }

    /** 当前登录账号信息（菜单 + 权限码），不重新下发 token。 */
    public LoginVO currentUserInfo() {
        return buildUserInfo(UserContext.required());
    }

    /** H5 用户接入：token 换取 user_id → 缓存基础信息 → 下发会话凭证。 */
    public AccessVO access(AccessDTO dto) {
        Tenant tenant = tenantService.getEnabledOrThrow(dto.getAppId());
        String userId = bizSystemClient.exchangeToken(tenant, dto.getToken());
        Customer customer = TenantContext.callWithAppId(tenant.getAppId(),
                () -> customerService.ensureCached(tenant, userId));
        String sessionToken = jwtUtil.generateSessionToken(tenant.getAppId(), userId);

        AccessVO vo = new AccessVO();
        vo.setSessionToken(sessionToken);
        vo.setAppId(tenant.getAppId());
        vo.setUserId(userId);
        vo.setCustomer(customerService.toVO(customer, false));
        return vo;
    }

    /** 组装登录/当前用户信息：按账号所属租户加载权限点与可见菜单树。 */
    private LoginVO buildUserInfo(LoginUser user) {
        LoginVO vo = new LoginVO();
        vo.setAccountId(user.getAccountId());
        vo.setAppId(user.getAppId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAccountType(user.getAccountType());
        // 角色/菜单查询走租户过滤，按账号 app_id 绑定上下文
        TenantContext.callWithAppId(user.getAppId(), () -> {
            vo.setPermCodes(permissionLoader.loadPermCodes(user.getAccountId(), user.getAppId()));
            vo.setMenus(menuService.treeForUser(user));
            return null;
        });
        return vo;
    }
}
