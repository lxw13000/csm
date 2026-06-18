package com.tsd.csm.modules.stats.controller;

import com.tsd.csm.core.common.exception.BizException;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.RequiresPermission;
import com.tsd.csm.modules.stats.domain.vo.AgentStatVO;
import com.tsd.csm.modules.stats.domain.vo.TicketStatsVO;
import com.tsd.csm.modules.stats.job.AgentWorkDailyJob;
import com.tsd.csm.modules.stats.service.StatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final AgentWorkDailyJob agentWorkDailyJob;

    public StatsController(StatsService statsService, AgentWorkDailyJob agentWorkDailyJob) {
        this.statsService = statsService;
        this.agentWorkDailyJob = agentWorkDailyJob;
    }

    /**
     * 工单统计（本租户，可选日期范围）。
     * @param startDate 起始日期（含），可空
     * @param endDate 截止日期（含），可空
     * @return 工单统计结果
     */
    @GetMapping("/ticket")
    @RequiresPermission("stats:ticket")
    public R<TicketStatsVO> ticket(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(statsService.ticketStats(startDate, endDate));
    }

    /**
     * 客服工作情况统计。
     * @param startDate 起始日期（含），可空
     * @param endDate 截止日期（含），可空
     * @param agentId 指定客服账号 id，可空表示全部
     * @return 客服统计列表
     */
    @GetMapping("/agent")
    @RequiresPermission("stats:agent")
    public R<List<AgentStatVO>> agent(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long agentId) {
        return R.ok(statsService.agentStats(startDate, endDate, agentId));
    }

    /**
     * 手动触发客服工作日汇总（即定时任务的聚合内容），按本租户指定日期范围重新统计。
     * 范围为空时默认统计当天（定时任务仅在每日凌晨统计前一日，今日数据需手动统计）。
     * @param startDate 起始日期（含），可空
     * @param endDate 截止日期（含），可空
     * @return 已聚合的天数
     */
    @PostMapping("/agent/aggregate")
    @RequiresPermission("stats:agent")
    public R<Integer> aggregateAgent(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now() : startDate;
        LocalDate end = endDate == null ? start : endDate;
        if (end.isBefore(start)) {
            throw new BizException("结束日期不能早于开始日期");
        }
        if (start.plusDays(92).isBefore(end)) {
            throw new BizException("单次手动统计跨度不能超过 92 天");
        }
        return R.ok(agentWorkDailyJob.aggregateCurrentTenant(start, end));
    }
}
