package com.tsd.csm.modules.customer.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.TimedEntity;

import java.time.LocalDateTime;

/**
 * C 端用户展示信息缓存（权威源为业务系统）。对应 csm_customer。
 */
@TableName("csm_customer")
public class Customer extends TimedEntity {

    /** 所属租户。 */
    private String appId;
    /** 业务系统用户 id。 */
    private String userId;
    /** 昵称（缓存）。 */
    private String nickname;
    /** 头像 URL（缓存）。 */
    private String avatar;
    /** 用户等级（缓存）。 */
    private String userLevel;
    /** 脱敏手机号（缓存）。 */
    private String maskedPhone;
    /** 注册时间（缓存）。 */
    private LocalDateTime registerTime;
    /** 最近一次同步业务系统信息的时间。 */
    private LocalDateTime lastSyncAt;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(String userLevel) {
        this.userLevel = userLevel;
    }

    public String getMaskedPhone() {
        return maskedPhone;
    }

    public void setMaskedPhone(String maskedPhone) {
        this.maskedPhone = maskedPhone;
    }

    public LocalDateTime getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(LocalDateTime registerTime) {
        this.registerTime = registerTime;
    }

    public LocalDateTime getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(LocalDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }
}
