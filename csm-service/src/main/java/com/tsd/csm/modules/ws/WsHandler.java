package com.tsd.csm.modules.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsd.csm.core.common.enums.ReaderType;
import com.tsd.csm.core.common.enums.WsChannelType;
import com.tsd.csm.core.realtime.RealtimeNotifier;
import com.tsd.csm.core.security.LoginUser;
import com.tsd.csm.core.tenant.TenantContext;
import com.tsd.csm.modules.ticket.domain.Ticket;
import com.tsd.csm.modules.ticket.domain.dto.SendMessageDTO;
import com.tsd.csm.modules.ticket.domain.vo.MessageVO;
import com.tsd.csm.modules.ticket.domain.vo.UserMessageResultVO;
import com.tsd.csm.modules.ticket.service.TicketService;
import com.tsd.csm.modules.ws.domain.WsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WebSocket 主处理器：连接注册/注销 + 按 type 路由（chat/typing/read/ping）。
 * 聊天复用 TicketService 的智能问答/回复编排，下行推送统一经 {@link RealtimeNotifier}（Redis 分发）。
 */
@Component
public class WsHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WsHandler.class);

    private static final String ATTR_KEY = "wsRegistryKey";

    private final ObjectMapper objectMapper;
    private final WsSessionRegistry sessionRegistry;
    private final TicketService ticketService;
    private final RealtimeNotifier notifier;

    public WsHandler(ObjectMapper objectMapper, WsSessionRegistry sessionRegistry,
                     TicketService ticketService, RealtimeNotifier notifier) {
        this.objectMapper = objectMapper;
        this.sessionRegistry = sessionRegistry;
        this.ticketService = ticketService;
        this.notifier = notifier;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        LoginUser user = loginUser(session);
        String key = registryKey(user);
        session.getAttributes().put(ATTR_KEY, key);
        sessionRegistry.register(key, session);
        log.debug("WebSocket 连接建立 key={} sessionId={}", key, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object key = session.getAttributes().get(ATTR_KEY);
        if (key != null) {
            sessionRegistry.unregister((String) key, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        LoginUser user = loginUser(session);
        TenantContext.setAppId(user.getAppId());
        try {
            WsMessage in = objectMapper.readValue(textMessage.getPayload(), WsMessage.class);
            WsChannelType type = WsChannelType.of(in.getType());
            if (type == null) {
                return;
            }
            switch (type) {
                case PING -> sendDirect(session, WsMessage.of(WsChannelType.PONG.getType(), null));
                case CHAT -> handleChat(session, user, in);
                case TYPING -> handleTyping(user, in);
                case READ -> handleRead(user, in);
                default -> {
                    // ack 等客户端确认无需服务端处理
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket 消息处理失败 sessionId={}, err={}", session.getId(), e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    private void handleChat(WebSocketSession session, LoginUser user, WsMessage in) {
        SendMessageDTO dto = new SendMessageDTO();
        dto.setContent(in.getContent());
        dto.setContentType(in.getContentType());
        dto.setClientMsgId(in.getClientMsgId());
        if (user.isCustomer()) {
            UserMessageResultVO result = ticketService.handleUserMessage(user.getUserId(), dto);
            ack(session, in.getClientMsgId(), result.getMessage().getSeq(), result.getMessage().getTicketId());
        } else {
            MessageVO message = ticketService.agentReply(in.getTicketId(), user.getAccountId(), dto);
            ack(session, in.getClientMsgId(), message.getSeq(), in.getTicketId());
        }
    }

    private void handleTyping(LoginUser user, WsMessage in) {
        Ticket ticket = ticketService.getById(in.getTicketId());
        if (ticket == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticketId", ticket.getId());
        if (user.isCustomer()) {
            if (ticket.getAgentId() != null) {
                payload.put("from", "user");
                notifier.toAgent(ticket.getAppId(), ticket.getAgentId(), WsChannelType.TYPING.getType(), payload);
            }
        } else {
            payload.put("from", "agent");
            notifier.toUser(ticket.getAppId(), ticket.getUserId(), WsChannelType.TYPING.getType(), payload);
        }
    }

    private void handleRead(LoginUser user, WsMessage in) {
        Ticket ticket = ticketService.getById(in.getTicketId());
        if (ticket == null || in.getSeq() == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticketId", ticket.getId());
        payload.put("seq", in.getSeq());
        if (user.isCustomer()) {
            ticketService.markRead(ticket.getId(), ReaderType.USER.getCode(), user.getUserId(), in.getSeq());
            if (ticket.getAgentId() != null) {
                notifier.toAgent(ticket.getAppId(), ticket.getAgentId(), WsChannelType.READ.getType(), payload);
            }
        } else {
            ticketService.markRead(ticket.getId(), ReaderType.AGENT.getCode(),
                    String.valueOf(user.getAccountId()), in.getSeq());
            notifier.toUser(ticket.getAppId(), ticket.getUserId(), WsChannelType.READ.getType(), payload);
        }
    }

    private void ack(WebSocketSession session, String clientMsgId, Long seq, Long ticketId) {
        WsMessage ack = new WsMessage();
        ack.setType(WsChannelType.ACK.getType());
        ack.setClientMsgId(clientMsgId);
        ack.setSeq(seq);
        ack.setTicketId(ticketId);
        sendDirect(session, ack);
    }

    private void sendDirect(WebSocketSession session, WsMessage message) {
        try {
            WsSessionRegistry.sendSafe(session, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.warn("WebSocket 直接发送失败 err={}", e.getMessage());
        }
    }

    private LoginUser loginUser(WebSocketSession session) {
        return (LoginUser) session.getAttributes().get(WsHandshakeInterceptor.ATTR_LOGIN_USER);
    }

    private String registryKey(LoginUser user) {
        return user.isCustomer()
                ? WsSessionRegistry.userKey(user.getAppId(), user.getUserId())
                : WsSessionRegistry.agentKey(user.getAppId(), user.getAccountId());
    }
}
