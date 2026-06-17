package com.tsd.csm.modules.account.controller;

import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.RequiresPermission;
import com.tsd.csm.modules.account.domain.Role;
import com.tsd.csm.modules.account.domain.dto.RoleSaveDTO;
import com.tsd.csm.modules.account.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色权限管理（本租户）。
 */
@RestController
@RequestMapping("/api/admin/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 本租户角色列表。
     * @return 角色列表
     */
    @GetMapping("/list")
    @RequiresPermission("system:role:list")
    public R<List<Role>> list() {
        return R.ok(roleService.listAll());
    }

    /**
     * 新增角色。
     * @param dto 角色信息（含菜单授权）
     * @return 新增的角色
     */
    @PostMapping
    @RequiresPermission("system:role:list")
    public R<Role> create(@RequestBody @Valid RoleSaveDTO dto) {
        return R.ok(roleService.create(dto));
    }

    /**
     * 编辑角色。
     * @param id 角色 id
     * @param dto 角色信息
     * @return 更新后的角色
     */
    @PutMapping("/{id}")
    @RequiresPermission("system:role:list")
    public R<Role> update(@PathVariable Long id, @RequestBody @Valid RoleSaveDTO dto) {
        return R.ok(roleService.update(id, dto));
    }

    /**
     * 删除角色（连带清除其菜单授权）。
     * @param id 角色 id
     */
    @DeleteMapping("/{id}")
    @RequiresPermission("system:role:list")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    /**
     * 查询角色已授权的菜单 id。
     * @param id 角色 id
     * @return 已授权菜单 id 列表
     */
    @GetMapping("/{id}/menus")
    @RequiresPermission("system:role:list")
    public R<List<Long>> menuIds(@PathVariable Long id) {
        return R.ok(roleService.menuIds(id));
    }

    /**
     * 重设角色的菜单授权（全量覆盖）。
     * @param id 角色 id
     * @param menuIds 菜单 id 列表
     */
    @PutMapping("/{id}/menus")
    @RequiresPermission("system:role:list")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return R.ok();
    }
}
