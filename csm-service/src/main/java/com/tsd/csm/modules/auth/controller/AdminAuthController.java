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
 * PC 管理端认证：平台超管与租户管理员登录。
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private static final Set<Integer> ADMIN_TYPES = Set.of(
            AccountType.PLATFORM_SUPER.getCode(), AccountType.TENANT_ADMIN.getCode());

    private final AuthService authService;

    public AdminAuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 平台超管 / 租户管理员登录。
     * @param dto 登录信息（appId + 用户名 + 密码）
     * @return 登录结果（含 token、菜单、权限码）
     */
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return R.ok(authService.login(dto, ADMIN_TYPES));
    }

    /**
     * 当前登录用户信息（菜单 + 权限码）。
     * @return 用户信息
     */
    @GetMapping("/me")
    public R<LoginVO> me() {
        return R.ok(authService.currentUserInfo());
    }

    /**
     * 登出（JWT 无状态，实际由客户端丢弃 token）。
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        // JWT 无状态，登出由客户端丢弃 token 即可
        return R.ok();
    }
}
