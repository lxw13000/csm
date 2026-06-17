package com.tsd.csm.modules.account.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.TimedEntity;

/**
 * 角色。对应 csm_role。
 */
@TableName("csm_role")
public class Role extends TimedEntity {

    /** 所属租户；平台级角色用 _platform_。 */
    private String appId;
    /** 角色名称。 */
    private String name;
    /** 角色编码。 */
    private String code;
    /** 备注。 */
    private String remark;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

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
}
