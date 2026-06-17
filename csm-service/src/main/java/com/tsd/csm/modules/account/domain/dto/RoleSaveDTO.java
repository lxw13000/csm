package com.tsd.csm.modules.account.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 角色新增/编辑入参（含菜单授权）。
 */
public class RoleSaveDTO {

    /** 角色名称。 */
    @NotBlank(message = "角色名称不能为空")
    private String name;

    /** 角色编码（本租户内唯一）。 */
    @NotBlank(message = "角色编码不能为空")
    private String code;

    /** 备注。 */
    private String remark;

    /** 授权的菜单 id 列表。 */
    private List<Long> menuIds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<Long> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<Long> menuIds) {
        this.menuIds = menuIds;
    }
}
