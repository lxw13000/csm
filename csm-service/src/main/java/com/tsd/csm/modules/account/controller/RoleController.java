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

    @GetMapping("/list")
    @RequiresPermission("system:role:list")
    public R<List<Role>> list() {
        return R.ok(roleService.listAll());
    }

    @PostMapping
    @RequiresPermission("system:role:list")
    public R<Role> create(@RequestBody @Valid RoleSaveDTO dto) {
        return R.ok(roleService.create(dto));
    }

    @PutMapping("/{id}")
    @RequiresPermission("system:role:list")
    public R<Role> update(@PathVariable Long id, @RequestBody @Valid RoleSaveDTO dto) {
        return R.ok(roleService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("system:role:list")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}/menus")
    @RequiresPermission("system:role:list")
    public R<List<Long>> menuIds(@PathVariable Long id) {
        return R.ok(roleService.menuIds(id));
    }

    @PutMapping("/{id}/menus")
    @RequiresPermission("system:role:list")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return R.ok();
    }
}
