package com.tsd.csm.modules.account.domain.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点 VO。
 */
public class MenuVO {

    /** 菜单 id。 */
    private Long id;
    /** 父菜单 id，0 为根。 */
    private Long parentId;
    /** 菜单/功能点名称。 */
    private String name;
    /** 类型：1 目录 / 2 菜单 / 3 按钮/权限点。 */
    private Integer type;
    /** 权限标识，如 ticket:list。 */
    private String permCode;
    /** 前端路由。 */
    private String path;
    /** 排序。 */
    private Integer sort;
    /** 子菜单节点。 */
    private List<MenuVO> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<MenuVO> getChildren() {
        return children;
    }

    public void setChildren(List<MenuVO> children) {
        this.children = children;
    }
}
