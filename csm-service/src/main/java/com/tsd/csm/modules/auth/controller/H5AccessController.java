package com.tsd.csm.modules.auth.controller;

import com.tsd.csm.core.common.result.R;
import com.tsd.csm.modules.auth.domain.dto.AccessDTO;
import com.tsd.csm.modules.auth.domain.vo.AccessVO;
import com.tsd.csm.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 H5 接入：app_id + 一次性 token 换取会话凭证（无需登录，见 WebMvcConfig 放行）。
 */
@RestController
@RequestMapping("/api/h5")
public class H5AccessController {

    private final AuthService authService;

    public H5AccessController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * H5 接入：app_id + 一次性 token 换取会话凭证。
     * @param dto 接入入参
     * @return 会话凭证与当前用户信息
     */
    @PostMapping("/access")
    public R<AccessVO> access(@RequestBody @Valid AccessDTO dto) {
        return R.ok(authService.access(dto));
    }
}
