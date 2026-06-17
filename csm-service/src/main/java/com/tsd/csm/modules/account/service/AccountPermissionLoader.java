package com.tsd.csm.modules.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tsd.csm.core.security.PermissionLoader;
import com.tsd.csm.modules.account.domain.AccountRole;
import com.tsd.csm.modules.account.domain.Menu;
import com.tsd.csm.modules.account.domain.RoleMenu;
import com.tsd.csm.modules.account.mapper.AccountRoleMapper;
import com.tsd.csm.modules.account.mapper.MenuMapper;
import com.tsd.csm.modules.account.mapper.RoleMenuMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限点加载器实现：账号 → 角色 → 角色菜单 → 权限点（perm_code）。
 * account_role / role_menu 的 app_id 由租户拦截器追加，保证按租户隔离。
 */
@Component
public class AccountPermissionLoader implements PermissionLoader {

    private final AccountRoleMapper accountRoleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;

    public AccountPermissionLoader(AccountRoleMapper accountRoleMapper, RoleMenuMapper roleMenuMapper,
                                   MenuMapper menuMapper) {
        this.accountRoleMapper = accountRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public Set<String> loadPermCodes(Long accountId, String appId) {
        if (accountId == null) {
            return Set.of();
        }
        List<Long> roleIds = accountRoleMapper
                .selectList(new LambdaQueryWrapper<AccountRole>().eq(AccountRole::getAccountId, accountId))
                .stream().map(AccountRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        List<Long> menuIds = roleMenuMapper
                .selectList(new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getRoleId, roleIds))
                .stream().map(RoleMenu::getMenuId).toList();
        if (menuIds.isEmpty()) {
            return Set.of();
        }
        return menuMapper.selectBatchIds(menuIds).stream()
                .map(Menu::getPermCode)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.toSet());
    }
}
