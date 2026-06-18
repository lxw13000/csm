package com.tsd.csm.modules.integration.service;

import com.tsd.csm.core.common.exception.BizException;
import com.tsd.csm.core.security.JwtUtil;
import com.tsd.csm.core.tenant.TenantContext;
import com.tsd.csm.modules.customer.domain.Customer;
import com.tsd.csm.modules.customer.service.CustomerService;
import com.tsd.csm.modules.integration.domain.dto.CredentialDTO;
import com.tsd.csm.modules.integration.domain.vo.CredentialVO;
import com.tsd.csm.modules.tenant.domain.Tenant;
import com.tsd.csm.modules.tenant.service.TenantService;
import org.springframework.stereotype.Service;

/**
 * 业务系统对接服务：颁发通信凭证。
 * 在一次调用内完成「app_id+app_secret 鉴权 → 同步缓存 C 端用户 → 按租户有效期签发凭证」（呼应 xuqiu.md 2.3/2.4）。
 */
@Service
public class IntegrationService {

    private final TenantService tenantService;
    private final CustomerService customerService;
    private final JwtUtil jwtUtil;

    public IntegrationService(TenantService tenantService, CustomerService customerService, JwtUtil jwtUtil) {
        this.tenantService = tenantService;
        this.customerService = customerService;
        this.jwtUtil = jwtUtil;
    }

    /** 颁发通信凭证：校验租户与密钥，同步缓存用户信息，按租户配置有效期签发凭证。 */
    public CredentialVO issueCredential(CredentialDTO dto) {
        Tenant tenant = tenantService.getEnabledOrThrow(dto.getAppId());
        if (!tenant.getAppSecret().equals(dto.getAppSecret())) {
            throw new BizException("app_secret 校验失败");
        }
        int expireMinutes = tenant.getCredentialExpireMinutes() == null ? 120 : tenant.getCredentialExpireMinutes();
        // 同步插入/更新 C 端用户缓存（在目标租户上下文中执行，使 app_id 写入正确）
        Customer customer = TenantContext.callWithAppId(tenant.getAppId(),
                () -> customerService.upsert(dto.getUserId(), dto.getNickname(), dto.getAvatar()));
        String credential = jwtUtil.generateSessionToken(tenant.getAppId(), dto.getUserId(), expireMinutes);

        CredentialVO vo = new CredentialVO();
        vo.setCredential(credential);
        vo.setAppId(tenant.getAppId());
        vo.setUserId(dto.getUserId());
        vo.setExpireMinutes(expireMinutes);
        vo.setCustomer(customerService.toVO(customer));
        return vo;
    }
}
