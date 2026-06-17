package com.tsd.csm.modules.integration.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 演示用「业务系统」mock 接口，模拟对接方实现的 ① 身份换取、② 查用户信息。
 * 仅用于本地联调（内置演示租户 biz_demo 的接口地址指向这里），生产由真实业务系统提供。
 *
 * <p>约定：身份换取直接以传入的 token 作为 userId 返回，便于用任意 user_id 调试。
 */
@RestController
@RequestMapping("/api/h5/mock")
public class MockBizController {

    /**
     * 模拟「身份换取」：直接以传入 token 作为 userId 返回。
     * @param body 含 token 的请求体
     * @return 含 userId 的响应
     */
    @PostMapping("/identity")
    public Map<String, Object> identity(@RequestBody Map<String, Object> body) {
        Object token = body.get("token");
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("userId", token == null ? "user_1001" : String.valueOf(token));
        return resp;
    }

    /**
     * 模拟「查用户信息」：按 userId 返回一组演示用户字段。
     * @param body 含 userId 的请求体
     * @return 演示用户信息
     */
    @PostMapping("/user-info")
    public Map<String, Object> userInfo(@RequestBody Map<String, Object> body) {
        String userId = String.valueOf(body.getOrDefault("userId", "user_1001"));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("userId", userId);
        resp.put("nickname", "演示用户" + userId);
        resp.put("avatar", "https://api.dicebear.com/7.x/miniavs/svg?seed=" + userId);
        resp.put("userLevel", "VIP1");
        resp.put("phone", "13800001111");
        resp.put("registerTime", "2024-01-01 10:00:00");
        return resp;
    }
}
