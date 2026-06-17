package com.tsd.csm.core.realtime;

/**
 * 实时下行推送抽象。由 ws 模块实现（避免 core/业务模块反向依赖 ws），
 * 派单、消息、工单状态变更等通过本接口推送，天然支持 Redis pub/sub 跨节点分发。
 */
public interface RealtimeNotifier {

    /** 推送给某 C 端用户（按 app_id + user_id 定位连接）。 */
    void toUser(String appId, String userId, String type, Object payload);

    /** 推送给某客服账号（按 app_id + accountId 定位连接）。 */
    void toAgent(String appId, Long accountId, String type, Object payload);
}
