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

    /**
     * 本租户全部角色（按 id 升序）。
     * @return 角色列表
     */
    public List<Role> listAll() {
        return lambdaQuery().orderByAsc(Role::getId).list();
    }

    /**
     * 新增角色（编码查重 + 菜单授权）。
     * @param dto 角色信息
     * @return 新增的角色
     * @throws BizException 角色编码已存在
     */
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

    /**
     * 编辑角色（menuIds 非空时一并重设菜单授权）。
     * @param id 角色 id
     * @param dto 角色信息
     * @return 更新后的角色
     */
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

    /**
     * 删除角色并清除其菜单授权。
     * @param id 角色 id
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getOwned(id);
        removeById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
    }

    /**
     * 查询角色已授权的菜单 id。
     * @param roleId 角色 id
     * @return 菜单 id 列表
     */
    public List<Long> menuIds(Long roleId) {
        getOwned(roleId);
        return roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId))
                .stream().map(RoleMenu::getMenuId).toList();
    }

    /**
     * 重设角色的菜单授权（先清后插，全量覆盖）。
     * @param roleId 角色 id
     * @param menuIds 菜单 id 列表（null 表示清空授权）
     */
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

    /** 取本租户内角色，不存在抛 404。 */
    private Role getOwned(Long id) {
        Role role = getById(id);
        if (role == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return role;
    }
}
