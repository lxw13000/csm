package com.tsd.csm.modules.auth.domain.vo;

import com.tsd.csm.modules.customer.domain.vo.CustomerVO;

/**
 * H5 接入返回体：客服系统自身的会话凭证 + 当前用户展示信息。
 * 当前工单与历史消息由后续 {@code /api/h5/ticket/current} 拉取（见 ticket 模块）。
 */
public class AccessVO {

    /** 客服系统自身的会话凭证（session token，JWT）。 */
    private String sessionToken;
    /** 所属租户。 */
    private String appId;
    /** 业务系统用户 id。 */
    private String userId;
    /** C 端用户展示信息（昵称/头像等）。 */
    private CustomerVO customer;

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

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

    public CustomerVO getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerVO customer) {
        this.customer = customer;
    }
}
