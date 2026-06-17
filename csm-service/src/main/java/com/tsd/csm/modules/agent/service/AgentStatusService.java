package com.tsd.csm.modules.agent.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsd.csm.core.common.enums.OnlineStatus;
import com.tsd.csm.modules.agent.domain.AgentStatus;
import com.tsd.csm.modules.agent.mapper.AgentStatusMapper;
import com.tsd.csm.modules.ticket.service.DispatchService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 客服在线状态服务：上线/下线，并在上线时触发队列工单重派。
 */
@Service
public class AgentStatusService extends ServiceImpl<AgentStatusMapper, AgentStatus> {

    private final DispatchService dispatchService;

    public AgentStatusService(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    public AgentStatus online(Long accountId) {
        AgentStatus status = getOrCreate(accountId);
        status.setOnlineStatus(OnlineStatus.ONLINE.getCode());
        status.setLastOnlineAt(LocalDateTime.now());
        updateById(status);
        // 新上线客服可能可承接队列中的工单
        dispatchService.dispatchQueued();
        return status;
    }

    public AgentStatus offline(Long accountId) {
        AgentStatus status = getOrCreate(accountId);
        status.setOnlineStatus(OnlineStatus.OFFLINE.getCode());
        updateById(status);
        return status;
    }

    public AgentStatus current(Long accountId) {
        return getOrCreate(accountId);
    }

    private AgentStatus getOrCreate(Long accountId) {
        AgentStatus status = lambdaQuery().eq(AgentStatus::getAccountId, accountId).one();
        if (status != null) {
            return status;
        }
        status = new AgentStatus();
        status.setAccountId(accountId);
        status.setOnlineStatus(OnlineStatus.OFFLINE.getCode());
        status.setCurrentLoad(0);
        save(status);
        return status;
    }
}
