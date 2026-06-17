package com.tsd.csm.modules.stats.controller;

import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.RequiresPermission;
import com.tsd.csm.modules.stats.domain.vo.AgentStatVO;
import com.tsd.csm.modules.stats.domain.vo.TicketStatsVO;
import com.tsd.csm.modules.stats.service.StatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计分析（本租户）：工单统计与客服工作情况统计。
 */
@RestController
@RequestMapping("/api/admin/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/ticket")
    @RequiresPermission("stats:ticket")
    public R<TicketStatsVO> ticket(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(statsService.ticketStats(startDate, endDate));
    }

    @GetMapping("/agent")
    @RequiresPermission("stats:agent")
    public R<List<AgentStatVO>> agent(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long agentId) {
        return R.ok(statsService.agentStats(startDate, endDate, agentId));
    }
}
