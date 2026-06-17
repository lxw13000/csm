package com.tsd.csm.modules.config.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 租户接入配置更新入参。
 */
public class TenantConfigUpdateDTO {

    /** 单客服最大同时接入量，0 = 不限制。 */
    @NotNull(message = "最大同时接入量不能为空")
    @Min(value = 0, message = "最大同时接入量不能为负，0 表示不限制")
    private Integer maxConcurrent;

    /** 用户无操作自动完结倒计时（分钟）。 */
    @NotNull(message = "自动完结时长不能为空")
    @Min(value = 1, message = "自动完结时长至少 1 分钟")
    private Integer autoCloseMinutes;

    /** 页面声音提醒开关：1 开 / 0 关。 */
    private Integer notifySound;

    /** 扩展配置（JSON 文本）。 */
    private String ext;

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
