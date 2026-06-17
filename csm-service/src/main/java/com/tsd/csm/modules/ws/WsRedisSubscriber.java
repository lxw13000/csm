package com.tsd.csm.modules.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsd.csm.modules.ws.domain.WsMessage;
import com.tsd.csm.modules.ws.domain.WsPushEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Redis 订阅者：收到下行推送信封后，在本节点查询会话注册表并投递（跨节点 fan-out 落地点）。
 */
@Component
public class WsRedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(WsRedisSubscriber.class);

    private final WsSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public WsRedisSubscriber(WsSessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            WsPushEnvelope envelope = objectMapper.readValue(
                    new String(message.getBody(), StandardCharsets.UTF_8), WsPushEnvelope.class);
            String key = "agent".equals(envelope.getTargetType())
                    ? WsSessionRegistry.agentKey(envelope.getAppId(), Long.valueOf(envelope.getTargetId()))
                    : WsSessionRegistry.userKey(envelope.getAppId(), envelope.getTargetId());

            Set<WebSocketSession> targets = sessionRegistry.find(key);
            if (targets.isEmpty()) {
                return;
            }
            String text = objectMapper.writeValueAsString(WsMessage.of(envelope.getType(), envelope.getPayload()));
            for (WebSocketSession session : targets) {
                WsSessionRegistry.sendSafe(session, text);
            }
        } catch (Exception e) {
            log.warn("WebSocket 下行投递失败 err={}", e.getMessage());
        }
    }
}
