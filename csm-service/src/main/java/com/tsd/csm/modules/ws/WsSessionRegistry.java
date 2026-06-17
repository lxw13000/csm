package com.tsd.csm.modules.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地 WebSocket 会话注册表（本节点内）。按 {@code appId+身份} 索引连接，
 * 跨节点投递由 {@link com.tsd.csm.modules.ws.WsRedisSubscriber} 在各节点查询本表完成。
 */
@Component
public class WsSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(WsSessionRegistry.class);

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public static String userKey(String appId, String userId) {
        return appId + ":user:" + userId;
    }

    public static String agentKey(String appId, Long accountId) {
        return appId + ":agent:" + accountId;
    }

    public void register(String key, WebSocketSession session) {
        sessions.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(String key, WebSocketSession session) {
        Set<WebSocketSession> set = sessions.get(key);
        if (set == null) {
            return;
        }
        set.remove(session);
        if (set.isEmpty()) {
            sessions.remove(key);
        }
    }

    public Set<WebSocketSession> find(String key) {
        return sessions.getOrDefault(key, Set.of());
    }

    /** 线程安全地发送文本（WebSocketSession.sendMessage 非线程安全，需串行化）。 */
    public static void sendSafe(WebSocketSession session, String text) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(text));
            }
        } catch (IOException e) {
            log.warn("WebSocket 发送失败 sessionId={}, err={}", session.getId(), e.getMessage());
        }
    }
}
