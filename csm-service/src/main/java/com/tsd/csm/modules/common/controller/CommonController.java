package com.tsd.csm.modules.common.controller;

import com.tsd.csm.core.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 通用接口：健康检查等（无需登录，见 WebMvcConfig 放行）。
 */
@RestController
@RequestMapping("/api/common")
public class CommonController {

    /**
     * 健康检查。
     * @return 服务存活标识
     */
    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        return R.ok(Map.of("status", "UP", "service", "csm-service"));
    }
}
