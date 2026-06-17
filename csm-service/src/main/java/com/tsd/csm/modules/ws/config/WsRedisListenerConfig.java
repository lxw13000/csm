package com.tsd.csm.modules.ws.config;

import com.tsd.csm.core.common.constant.CsmConst;
import com.tsd.csm.modules.ws.WsRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 订阅 WebSocket 下行推送频道，使本节点收到任意节点发布的推送并投递本地连接。
 */
@Configuration
public class WsRedisListenerConfig {

    @Bean
    public RedisMessageListenerContainer wsRedisListenerContainer(RedisConnectionFactory connectionFactory,
                                                                  WsRedisSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(CsmConst.REDIS_WS_PUSH_CHANNEL));
        return container;
    }
}
