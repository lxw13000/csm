package com.tsd.csm.modules.auth.controller;

import com.tsd.csm.core.common.enums.AccountType;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.modules.auth.domain.dto.LoginDTO;
import com.tsd.csm.modules.auth.domain.vo.LoginVO;
import com.tsd.csm.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * 客服端 H5 认证：仅客服账号（account_type=3）可登录。
 */
@RestController
@RequestMapping("/api/agent/auth")
public class AgentAuthController {

    private static final Set<Integer> AGENT_TYPES = Set.of(AccountType.AGENT.getCode());

    private final AuthService authService;

    public AgentAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return R.ok(authService.login(dto, AGENT_TYPES));
    }

    @GetMapping("/me")
    public R<LoginVO> me() {
        return R.ok(authService.currentUserInfo());
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }
}
