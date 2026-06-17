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

    /**
     * 客服上线：置为在线并记录上线时间，随后重派队列中的工单。
     * @param accountId 客服账号 id
     * @return 更新后的客服状态
     */
    public AgentStatus online(Long accountId) {
        AgentStatus status = getOrCreate(accountId);
        status.setOnlineStatus(OnlineStatus.ONLINE.getCode());
        status.setLastOnlineAt(LocalDateTime.now());
        updateById(status);
        // 新上线客服可能可承接队列中的工单
        dispatchService.dispatchQueued();
        return status;
    }

    /**
     * 客服下线：仅置为离线，不影响其已接工单。
     * @param accountId 客服账号 id
     * @return 更新后的客服状态
     */
    public AgentStatus offline(Long accountId) {
        AgentStatus status = getOrCreate(accountId);
        status.setOnlineStatus(OnlineStatus.OFFLINE.getCode());
        updateById(status);
        return status;
    }

    /**
     * 查询客服当前状态（不存在则初始化为离线）。
     * @param accountId 客服账号 id
     * @return 客服状态
     */
    public AgentStatus current(Long accountId) {
        return getOrCreate(accountId);
    }

    /** 取客服状态行，不存在则新建一条离线、零负载的记录。 */
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
