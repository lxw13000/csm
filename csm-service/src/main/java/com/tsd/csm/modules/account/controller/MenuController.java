package com.tsd.csm.modules.account.controller;

import com.tsd.csm.core.common.enums.AccountType;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.RequireRole;
import com.tsd.csm.core.security.RequiresPermission;
import com.tsd.csm.modules.account.domain.Menu;
import com.tsd.csm.modules.account.domain.dto.MenuSaveDTO;
import com.tsd.csm.modules.account.domain.vo.MenuVO;
import com.tsd.csm.modules.account.service.MenuService;
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
 * 菜单管理（平台全局字典）。读取按角色裁剪，写操作仅平台超管。
 */
@RestController
@RequestMapping("/api/admin/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /** 完整菜单树（菜单管理页用）。 */
    @GetMapping("/tree")
    @RequiresPermission("system:menu:list")
    public R<List<MenuVO>> tree() {
        return R.ok(menuService.fullTree());
    }

    /**
     * 新增菜单（仅平台超管）。
     * @param dto 菜单信息
     * @return 新增的菜单
     */
    @PostMapping
    @RequireRole(AccountType.PLATFORM_SUPER)
    public R<Menu> create(@RequestBody @Valid MenuSaveDTO dto) {
        Menu menu = apply(new Menu(), dto);
        menuService.save(menu);
        return R.ok(menu);
    }

    /**
     * 编辑菜单（仅平台超管）。
     * @param id 菜单 id
     * @param dto 菜单信息
     * @return 更新后的菜单
     */
    @PutMapping("/{id}")
    @RequireRole(AccountType.PLATFORM_SUPER)
    public R<Menu> update(@PathVariable Long id, @RequestBody @Valid MenuSaveDTO dto) {
        Menu menu = apply(new Menu(), dto);
        menu.setId(id);
        menuService.updateById(menu);
        return R.ok(menu);
    }

    /**
     * 删除菜单（仅平台超管）。
     * @param id 菜单 id
     */
    @DeleteMapping("/{id}")
    @RequireRole(AccountType.PLATFORM_SUPER)
    public R<Void> delete(@PathVariable Long id) {
        menuService.removeById(id);
        return R.ok();
    }

    /** 将入参拷贝到菜单实体，parentId/sort 缺省补 0。 */
    private Menu apply(Menu menu, MenuSaveDTO dto) {
        menu.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        menu.setName(dto.getName());
        menu.setType(dto.getType());
        menu.setPermCode(dto.getPermCode());
        menu.setPath(dto.getPath());
        menu.setSort(dto.getSort() == null ? 0 : dto.getSort());
        return menu;
    }
}
