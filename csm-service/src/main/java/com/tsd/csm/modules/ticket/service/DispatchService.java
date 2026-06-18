package com.tsd.csm.modules.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tsd.csm.core.common.enums.OnlineStatus;
import com.tsd.csm.core.common.enums.TicketStatus;
import com.tsd.csm.core.common.enums.WsChannelType;
import com.tsd.csm.core.realtime.RealtimeNotifier;
import com.tsd.csm.core.tenant.TenantContext;
import com.tsd.csm.modules.agent.domain.AgentStatus;
import com.tsd.csm.modules.agent.mapper.AgentStatusMapper;
import com.tsd.csm.modules.config.domain.TenantConfig;
import com.tsd.csm.modules.config.service.TenantConfigService;
import com.tsd.csm.modules.ticket.domain.Ticket;
import com.tsd.csm.modules.ticket.mapper.TicketMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能派单引擎（xuqiu.md 5.4）：同租户 + 在线 + 未达接入阈值（0=不限）+ 负载最小者。
 *
 * <p>并发一致性：Redis 锁按租户串行化派单临界区，库内再以「乐观条件更新 current_load」兜底，
 * 杜绝超卖与同一工单重复派单。派单触发点：转人工、客服上线、工单完结释放容量。
 */
@Service
public class DispatchService {

    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

    private static final long LOCK_TTL_MILLIS = 5000L;
    private static final long LOCK_WAIT_MILLIS = 3000L;

    private final TicketMapper ticketMapper;
    private final AgentStatusMapper agentStatusMapper;
    private final TenantConfigService tenantConfigService;
    private final com.tsd.csm.core.util.RedisLock redisLock;
    private final RealtimeNotifier notifier;

    public DispatchService(TicketMapper ticketMapper, AgentStatusMapper agentStatusMapper,
                           TenantConfigService tenantConfigService, com.tsd.csm.core.util.RedisLock redisLock,
                           RealtimeNotifier notifier) {
        this.ticketMapper = ticketMapper;
        this.agentStatusMapper = agentStatusMapper;
        this.tenantConfigService = tenantConfigService;
        this.redisLock = redisLock;
        this.notifier = notifier;
    }

    /** 给单个排队工单派单；成功返回 true，无可用客服返回 false（保持排队）。 */
    public boolean dispatch(Long ticketId) {
        String appId = TenantContext.getAppId();
        String lockKey = "csm:dispatch:lock:" + appId;
        return redisLock.executeLocked(lockKey, LOCK_TTL_MILLIS, LOCK_WAIT_MILLIS, () -> doDispatch(appId, ticketId));
    }

    /** 容量释放或客服上线后，按创建时间顺序重派本租户队列中「未分配」的工单，直到无容量。 */
    public void dispatchQueued() {
        List<Ticket> queued = ticketMapper.selectList(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getStatus, TicketStatus.TRANSFERRING.getCode())
                .isNull(Ticket::getAgentId)
                .orderByAsc(Ticket::getCreatedAt));
        for (Ticket ticket : queued) {
            if (!dispatch(ticket.getId())) {
                break;
            }
        }
    }

    /** 转接：无条件给目标客服 +1 负载。 */
    public void incrementLoad(Long agentId) {
        agentStatusMapper.update(null, new LambdaUpdateWrapper<AgentStatus>()
                .eq(AgentStatus::getAccountId, agentId)
                .setSql("current_load = current_load + 1"));
    }

    /** 完结/转出：给客服 -1 负载（不低于 0）。 */
    public void releaseLoad(Long agentId) {
        agentStatusMapper.update(null, new LambdaUpdateWrapper<AgentStatus>()
                .eq(AgentStatus::getAccountId, agentId)
                .gt(AgentStatus::getCurrentLoad, 0)
                .setSql("current_load = current_load - 1"));
    }

    /** 该客服是否在线（本租户）。 */
    public boolean isOnline(Long agentId) {
        AgentStatus status = agentStatusMapper.selectOne(new LambdaQueryWrapper<AgentStatus>()
                .eq(AgentStatus::getAccountId, agentId));
        return status != null && status.getOnlineStatus() != null
                && status.getOnlineStatus() == OnlineStatus.ONLINE.getCode();
    }

    private boolean doDispatch(String appId, Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null || ticket.getStatus() == null
                || ticket.getStatus() != TicketStatus.TRANSFERRING.getCode()
                || ticket.getAgentId() != null) {
            // 已被处理、状态不符或已分配，幂等返回
            return false;
        }
        TenantConfig config = tenantConfigService.getCurrent();
        int max = config.getMaxConcurrent() == null ? 0 : config.getMaxConcurrent();

        LambdaQueryWrapper<AgentStatus> wrapper = new LambdaQueryWrapper<AgentStatus>()
                .eq(AgentStatus::getOnlineStatus, OnlineStatus.ONLINE.getCode())
                .orderByAsc(AgentStatus::getCurrentLoad);
        if (max > 0) {
            wrapper.lt(AgentStatus::getCurrentLoad, max);
        }
        List<AgentStatus> candidates = agentStatusMapper.selectList(wrapper);

        for (AgentStatus candidate : candidates) {
            Integer load = candidate.getCurrentLoad();
            int affected = agentStatusMapper.update(null, new LambdaUpdateWrapper<AgentStatus>()
                    .eq(AgentStatus::getAccountId, candidate.getAccountId())
                    .eq(AgentStatus::getCurrentLoad, load)
                    .setSql("current_load = current_load + 1"));
            if (affected != 1) {
                continue; // 负载被并发改动，尝试下一个候选
            }
            // 仅「分配」给客服（记录处理人 + 占用容量），状态仍保持「人工转接中」；
            // 待客服点开工单（accept）才转为「处理中」，即真正接入人工（xuqiu.md 4.2 / 5.4）。
            ticket.setAgentId(candidate.getAccountId());
            ticket.setAssignedAt(LocalDateTime.now());
            ticketMapper.updateById(ticket);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("ticketId", ticketId);
            payload.put("userId", ticket.getUserId());
            payload.put("status", TicketStatus.TRANSFERRING.getCode());
            notifier.toAgent(appId, candidate.getAccountId(), WsChannelType.NOTIFICATION.getType(), payload);
            notifier.toAgent(appId, candidate.getAccountId(), WsChannelType.TICKET_STATUS.getType(), payload);
            log.debug("派单成功（已分配，待接入）app={} ticket={} -> agent={}", appId, ticketId, candidate.getAccountId());
            return true;
        }
        log.debug("派单暂无可用客服 app={} ticket={}", appId, ticketId);
        return false;
    }
}
