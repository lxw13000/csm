package com.tsd.csm.modules.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsd.csm.core.security.LoginUser;
import com.tsd.csm.modules.account.domain.AccountRole;
import com.tsd.csm.modules.account.domain.Menu;
import com.tsd.csm.modules.account.domain.RoleMenu;
import com.tsd.csm.modules.account.domain.vo.MenuVO;
import com.tsd.csm.modules.account.mapper.AccountRoleMapper;
import com.tsd.csm.modules.account.mapper.MenuMapper;
import com.tsd.csm.modules.account.mapper.RoleMenuMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单/权限点服务。{@code csm_menu} 为全局字典（租户拦截器忽略），按账号角色裁剪可见范围。
 */
@Service
public class MenuService extends ServiceImpl<MenuMapper, Menu> {

    private final AccountRoleMapper accountRoleMapper;
    private final RoleMenuMapper roleMenuMapper;

    public MenuService(AccountRoleMapper accountRoleMapper, RoleMenuMapper roleMenuMapper) {
        this.accountRoleMapper = accountRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    /** 全部菜单，按 sort 升序。 */
    public List<Menu> listAll() {
        return lambdaQuery().orderByAsc(Menu::getSort).list();
    }

    /** 当前登录用户可见的菜单树。 */
    public List<MenuVO> treeForUser(LoginUser user) {
        List<Menu> all = listAll();
        List<Menu> visible;
        if (user.isPlatformSuper()) {
            visible = all;
        } else {
            Set<Long> menuIds = accessibleMenuIds(user.getAccountId());
            // 租户管理员未配置受限角色（无角色菜单授权）视为本租户全权（与 PermissionAspect 一致）
            if (menuIds.isEmpty() && user.isTenantAdmin()) {
                visible = all;
            } else {
                visible = all.stream().filter(m -> menuIds.contains(m.getId())).toList();
            }
        }
        return buildTree(visible);
    }

    /** 完整菜单树（平台超管菜单管理用）。 */
    public List<MenuVO> fullTree() {
        return buildTree(listAll());
    }

    /** 账号经由角色授权可访问的菜单 id 集合。 */
    public Set<Long> accessibleMenuIds(Long accountId) {
        if (accountId == null) {
            return Set.of();
        }
        List<Long> roleIds = accountRoleMapper
                .selectList(new LambdaQueryWrapper<AccountRole>().eq(AccountRole::getAccountId, accountId))
                .stream().map(AccountRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return roleMenuMapper
                .selectList(new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getRoleId, roleIds))
                .stream().map(RoleMenu::getMenuId).collect(Collectors.toSet());
    }

    /** 将扁平菜单列表组装为树；父节点缺失的节点挂到根，避免丢失。 */
    private List<MenuVO> buildTree(List<Menu> menus) {
        List<MenuVO> nodes = menus.stream().map(this::toVO).toList();
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO node : nodes) {
            if (node.getParentId() == null || node.getParentId() == 0L) {
                roots.add(node);
            } else {
                nodes.stream()
                        .filter(parent -> parent.getId().equals(node.getParentId()))
                        .findFirst()
                        .ifPresentOrElse(parent -> parent.getChildren().add(node), () -> roots.add(node));
            }
        }
        return roots;
    }

    private MenuVO toVO(Menu menu) {
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getName());
        vo.setType(menu.getType());
        vo.setPermCode(menu.getPermCode());
        vo.setPath(menu.getPath());
        vo.setSort(menu.getSort());
        return vo;
    }
}
