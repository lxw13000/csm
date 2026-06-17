package com.tsd.csm.modules.config.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.TimedEntity;

/**
 * 租户级配置。对应 csm_tenant_config。
 */
@TableName("csm_tenant_config")
public class TenantConfig extends TimedEntity {

    /** 所属租户。 */
    private String appId;
    /** 单客服最大同时接入量，0 = 不限制。 */
    private Integer maxConcurrent;
    /** 用户无操作自动完结倒计时（分钟）。 */
    private Integer autoCloseMinutes;
    /** 页面声音提醒开关：1 开 / 0 关。 */
    private Integer notifySound;
    /** 扩展配置（JSON 文本，提醒策略等）。 */
    private String ext;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Integer getMaxConcurrent() {
        return maxConcurrent;
    }

    public void setMaxConcurrent(Integer maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }

    public Integer getAutoCloseMinutes() {
        return autoCloseMinutes;
    }

    public void setAutoCloseMinutes(Integer autoCloseMinutes) {
        this.autoCloseMinutes = autoCloseMinutes;
    }

    public Integer getNotifySound() {
        return notifySound;
    }

    public void setNotifySound(Integer notifySound) {
        this.notifySound = notifySound;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }
}
