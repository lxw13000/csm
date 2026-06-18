package com.tsd.csm.modules.integration.controller;

import com.tsd.csm.core.common.result.R;
import com.tsd.csm.modules.integration.domain.dto.CredentialDTO;
import com.tsd.csm.modules.integration.domain.vo.CredentialVO;
import com.tsd.csm.modules.integration.service.IntegrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务系统对接接口：颁发通信凭证（无需登录，见 WebMvcConfig 放行）。
 * 业务系统后端用 app_id + app_secret 调用本接口换取凭证，每次进入客服系统时调用。
 */
@RestController
@RequestMapping("/api/integration")
public class CredentialController {

    private final IntegrationService integrationService;

    public CredentialController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    /**
     * 颁发通信凭证。
     * @param dto app_id + app_secret + user_id（必填）+ 昵称/头像（选填）
     * @return 通信凭证与有效期
     */
    @PostMapping("/credential")
    public R<CredentialVO> credential(@RequestBody @Valid CredentialDTO dto) {
        return R.ok(integrationService.issueCredential(dto));
    }
}
