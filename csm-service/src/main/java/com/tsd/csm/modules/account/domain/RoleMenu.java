package com.tsd.csm.modules.account.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.BaseEntity;

/**
 * 角色-菜单关联。对应 csm_role_menu。
 */
@TableName("csm_role_menu")
public class RoleMenu extends BaseEntity {

    /** 所属租户（平台级为 _platform_）。 */
    private String appId;
    /** 角色 id。 */
    private Long roleId;
    /** 菜单 id。 */
    private Long menuId;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }
}
