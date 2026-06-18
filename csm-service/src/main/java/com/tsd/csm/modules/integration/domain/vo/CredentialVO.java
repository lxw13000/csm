package com.tsd.csm.modules.integration.domain.vo;

import com.tsd.csm.modules.customer.domain.vo.CustomerVO;

/**
 * 颁发通信凭证返回体：客服系统签发的通信凭证（session token）及其有效期 + 当前用户展示信息。
 * 业务系统将 {@code credential} 通过 WebView URL 传给用户端 H5，H5 直接据此建立 WebSocket、收发消息。
 */
public class CredentialVO {

    /** 通信凭证（session token，JWT）。 */
    private String credential;
    /** 所属租户。 */
    private String appId;
    /** 业务系统用户 id。 */
    private String userId;
    /** 凭证有效期（分钟）。 */
    private Integer expireMinutes;
    /** C 端用户展示信息（昵称/头像等）。 */
    private CustomerVO customer;

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
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

    public Integer getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(Integer expireMinutes) {
        this.expireMinutes = expireMinutes;
    }

    public CustomerVO getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerVO customer) {
        this.customer = customer;
    }
}
