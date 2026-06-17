package com.tsd.csm.modules.integration.client;

import com.tsd.csm.core.common.exception.BizException;
import com.tsd.csm.core.util.SignUtil;
import com.tsd.csm.modules.integration.domain.CustomerInfo;
import com.tsd.csm.modules.tenant.domain.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务系统出站调用客户端（对接适配层）。按租户配置的接口地址路由，请求带 app_secret 签名。
 * 对接方实现两类接口：① 身份换取（token→user_id），② 按 user_id 查用户信息。
 */
@Component
public class BizSystemClient {

    private static final Logger log = LoggerFactory.getLogger(BizSystemClient.class);

    private final RestClient restClient = RestClient.create();

    /** ① 身份换取：用一次性 token 换取 user_id。失败抛异常（接入即拒绝）。 */
    public String exchangeToken(Tenant tenant, String token) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("appId", tenant.getAppId());
        params.put("token", token);
        params.put("sign", SignUtil.sign(params, tenant.getAppSecret()));
        try {
            Map<?, ?> resp = restClient.post()
                    .uri(tenant.getIdentityApi())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(params)
                    .retrieve()
                    .body(Map.class);
            Object userId = resp == null ? null : resp.get("userId");
            if (userId == null) {
                throw new BizException("身份换取失败：业务系统未返回 userId");
            }
            return String.valueOf(userId);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("身份换取调用失败 app_id={}, err={}", tenant.getAppId(), e.getMessage());
            throw new BizException("身份换取失败：" + e.getMessage());
        }
    }

    /** ② 按 user_id 查用户信息。失败返回 null（调用方降级到缓存）。 */
    public CustomerInfo queryUserInfo(Tenant tenant, String userId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("appId", tenant.getAppId());
        params.put("userId", userId);
        params.put("sign", SignUtil.sign(params, tenant.getAppSecret()));
        try {
            return restClient.post()
                    .uri(tenant.getUserInfoApi())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(params)
                    .retrieve()
                    .body(CustomerInfo.class);
        } catch (Exception e) {
            log.warn("查询用户信息失败 app_id={}, userId={}, err={}", tenant.getAppId(), userId, e.getMessage());
            return null;
        }
    }
}
