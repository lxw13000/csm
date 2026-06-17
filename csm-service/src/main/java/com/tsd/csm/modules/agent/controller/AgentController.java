package com.tsd.csm.modules.agent.controller;

import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.LoginUser;
import com.tsd.csm.core.security.UserContext;
import com.tsd.csm.modules.agent.domain.AgentStatus;
import com.tsd.csm.modules.agent.service.AgentStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客服端 H5 上下线与状态。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentStatusService agentStatusService;

    public AgentController(AgentStatusService agentStatusService) {
        this.agentStatusService = agentStatusService;
    }

    @PostMapping("/online")
    public R<AgentStatus> online() {
        return R.ok(agentStatusService.online(currentAgentId()));
    }

    @PostMapping("/offline")
    public R<AgentStatus> offline() {
        return R.ok(agentStatusService.offline(currentAgentId()));
    }

    @GetMapping("/status")
    public R<AgentStatus> status() {
        return R.ok(agentStatusService.current(currentAgentId()));
    }

    private Long currentAgentId() {
        LoginUser user = UserContext.required();
        return user.getAccountId();
    }
}
