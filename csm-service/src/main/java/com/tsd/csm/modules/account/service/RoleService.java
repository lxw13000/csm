package com.tsd.csm.modules.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsd.csm.core.common.exception.BizException;
import com.tsd.csm.core.common.result.ResultCode;
import com.tsd.csm.modules.account.domain.Role;
import com.tsd.csm.modules.account.domain.RoleMenu;
import com.tsd.csm.modules.account.domain.dto.RoleSaveDTO;
import com.tsd.csm.modules.account.mapper.RoleMapper;
import com.tsd.csm.modules.account.mapper.RoleMenuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务（本租户 RBAC）。角色及其菜单授权按 app_id 隔离。
 */
@Service
public class RoleService extends ServiceImpl<RoleMapper, Role> {

    private final RoleMenuMapper roleMenuMapper;

    public RoleService(RoleMenuMapper roleMenuMapper) {
        this.roleMenuMapper = roleMenuMapper;
    }

    public List<Role> listAll() {
        return lambdaQuery().orderByAsc(Role::getId).list();
    }

    @Transactional(rollbackFor = Exception.class)
    public Role create(RoleSaveDTO dto) {
        if (lambdaQuery().eq(Role::getCode, dto.getCode()).count() > 0) {
            throw new BizException("角色编码已存在：" + dto.getCode());
        }
        Role role = new Role();
        role.setName(dto.getName());
        role.setCode(dto.getCode());
        role.setRemark(dto.getRemark());
        save(role);
        assignMenus(role.getId(), dto.getMenuIds());
        return role;
    }

    @Transactional(rollbackFor = Exception.class)
    public Role update(Long id, RoleSaveDTO dto) {
        Role role = getOwned(id);
        role.setName(dto.getName());
        role.setRemark(dto.getRemark());
        updateById(role);
        if (dto.getMenuIds() != null) {
            assignMenus(id, dto.getMenuIds());
        }
        return role;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getOwned(id);
        removeById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
    }

    public List<Long> menuIds(Long roleId) {
        getOwned(roleId);
        return roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId))
                .stream().map(RoleMenu::getMenuId).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        if (menuIds == null) {
            return;
        }
        for (Long menuId : menuIds) {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenuMapper.insert(roleMenu);
        }
    }

    private Role getOwned(Long id) {
        Role role = getById(id);
        if (role == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return role;
    }
}
