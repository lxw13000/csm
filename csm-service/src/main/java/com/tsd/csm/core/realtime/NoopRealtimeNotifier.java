package com.tsd.csm.core.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 默认空实现：仅记录日志。当 ws 模块的 {@code WsPushService}（{@code @Primary}）存在时被其覆盖，
 * 保证未启用 WebSocket 时业务逻辑（派单/完结）仍可正常运行。
 */
@Component
public class NoopRealtimeNotifier implements RealtimeNotifier {

    private static final Logger log = LoggerFactory.getLogger(NoopRealtimeNotifier.class);

    @Override
    public void toUser(String appId, String userId, String type, Object payload) {
        log.debug("[noop-notify] user app={} userId={} type={} payload={}", appId, userId, type, payload);
    }

    @Override
    public void toAgent(String appId, Long accountId, String type, Object payload) {
        log.debug("[noop-notify] agent app={} accountId={} type={} payload={}", appId, accountId, type, payload);
    }
}
