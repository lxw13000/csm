package com.tsd.csm.modules.ws;

import com.tsd.csm.core.security.JwtUtil;
import com.tsd.csm.core.security.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手鉴权：从 query 参数 {@code token} 解析登录主体（会话凭证或客服账号 token）。
 * 解析失败拒绝握手；成功则将 {@link LoginUser} 放入会话属性供 {@link WsHandler} 使用。
 */
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_LOGIN_USER = "wsLoginUser";

    private final JwtUtil jwtUtil;

    public WsHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request.getURI().getQuery());
        LoginUser user = token == null ? null : jwtUtil.parse(token);
        if (user == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(ATTR_LOGIN_USER, user);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    /** 从 query string 中提取 token 参数，缺失返回 null。 */
    private String resolveToken(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0 && "token".equals(pair.substring(0, idx))) {
                return pair.substring(idx + 1);
            }
        }
        return null;
    }
}
