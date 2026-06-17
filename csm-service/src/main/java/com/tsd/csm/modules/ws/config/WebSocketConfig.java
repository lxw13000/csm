package com.tsd.csm.modules.ws.config;

import com.tsd.csm.modules.ws.WsHandler;
import com.tsd.csm.modules.ws.WsHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 端点注册：单连接 {@code /ws}，握手鉴权由 {@link WsHandshakeInterceptor} 完成。
 * {@code /ws} 不在 {@code /api/**} 之下，不经 AuthInterceptor，鉴权在握手层独立处理。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WsHandler wsHandler;
    private final WsHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(WsHandler wsHandler, WsHandshakeInterceptor handshakeInterceptor) {
        this.wsHandler = wsHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(wsHandler, "/ws")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
