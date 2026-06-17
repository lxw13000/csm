package com.tsd.csm.core.common.constant;

/**
 * 全局常量。
 */
public final class CsmConst {

    private CsmConst() {
    }

    /** 平台级保留 app_id（平台超管、全局角色等使用）。 */
    public static final String PLATFORM_APP_ID = "_platform_";

    /** 请求头：登录态 token。 */
    public static final String HEADER_TOKEN = "Authorization";

    /** Bearer 前缀。 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 请求头：平台超管「当前选中租户」。 */
    public static final String HEADER_APP_ID = "X-App-Id";

    /** 请求属性：当前登录用户。 */
    public static final String ATTR_LOGIN_USER = "csm.loginUser";

    /** Redis key 前缀：H5 会话凭证。 */
    public static final String REDIS_SESSION_PREFIX = "csm:session:";

    /** Redis key 前缀：客服在线状态镜像。 */
    public static final String REDIS_AGENT_ONLINE_PREFIX = "csm:agent:online:";

    /** Redis key 前缀：工单会话内消息序号。 */
    public static final String REDIS_TICKET_SEQ_PREFIX = "csm:ticket:seq:";

    /** Redis pub/sub 频道：WebSocket 跨节点下行推送。 */
    public static final String REDIS_WS_PUSH_CHANNEL = "csm:ws:push";
}
