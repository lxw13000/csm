package com.tsd.csm.modules.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsd.csm.core.common.constant.CsmConst;
import com.tsd.csm.core.realtime.RealtimeNotifier;
import com.tsd.csm.modules.ws.domain.WsPushEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 实时下行推送实现（{@code @Primary}，覆盖 NoopRealtimeNotifier）：
 * 仅将信封发布到 Redis 频道，由各节点（含本节点）的 {@link WsRedisSubscriber} 投递本地连接，
 * 保证单节点不重复投递且天然跨节点。
 */
@Service
@Primary
public class WsPushService implements RealtimeNotifier {

    private static final Logger log = LoggerFactory.getLogger(WsPushService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public WsPushService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void toUser(String appId, String userId, String type, Object payload) {
        publish(new WsPushEnvelope("user", appId, userId, type, payload));
    }

    @Override
    public void toAgent(String appId, Long accountId, String type, Object payload) {
        publish(new WsPushEnvelope("agent", appId, String.valueOf(accountId), type, payload));
    }

    private void publish(WsPushEnvelope envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            stringRedisTemplate.convertAndSend(CsmConst.REDIS_WS_PUSH_CHANNEL, json);
        } catch (Exception e) {
            log.warn("WebSocket 推送发布失败 type={} target={}:{}, err={}",
                    envelope.getType(), envelope.getTargetType(), envelope.getTargetId(), e.getMessage());
        }
    }
}
