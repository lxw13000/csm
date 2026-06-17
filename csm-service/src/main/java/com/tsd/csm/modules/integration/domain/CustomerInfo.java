package com.tsd.csm.modules.integration.domain;

/**
 * 业务系统返回的 C 端用户信息（接口②的解析结果）。
 * 手机号为原始值，缓存前由服务端脱敏（见 customer 模块）。
 */
public class CustomerInfo {

    /** 用户 id。 */
    private String userId;
    /** 昵称。 */
    private String nickname;
    /** 头像 URL。 */
    private String avatar;
    /** 用户等级。 */
    private String userLevel;
    /** 手机号（原始值，缓存前由服务端脱敏）。 */
    private String phone;
    /** 注册时间。 */
    private String registerTime;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(String registerTime) {
        this.registerTime = registerTime;
    }
}
