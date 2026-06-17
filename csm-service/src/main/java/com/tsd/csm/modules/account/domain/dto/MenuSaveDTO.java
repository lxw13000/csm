package com.tsd.csm.modules.account.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 菜单/权限点新增/编辑入参（平台全局字典，平台超管维护）。
 */
public class MenuSaveDTO {

    /** 父菜单 id，0 或空为根。 */
    private Long parentId;

    /** 菜单/功能点名称。 */
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    /** 类型：1 目录 / 2 菜单 / 3 按钮/权限点。 */
    @NotNull(message = "菜单类型不能为空")
    private Integer type;

    /** 权限标识，如 ticket:list。 */
    private String permCode;

    /** 前端路由。 */
    private String path;

    /** 排序。 */
    private Integer sort;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getPermCode() {
        return permCode;
    }

    public void setPermCode(String permCode) {
        this.permCode = permCode;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
