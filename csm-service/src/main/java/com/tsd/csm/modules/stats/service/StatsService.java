package com.tsd.csm.modules.stats.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsd.csm.core.common.enums.TicketStatus;
import com.tsd.csm.modules.account.domain.Account;
import com.tsd.csm.modules.account.mapper.AccountMapper;
import com.tsd.csm.modules.stats.domain.AgentWorkDaily;
import com.tsd.csm.modules.stats.domain.vo.AgentStatVO;
import com.tsd.csm.modules.stats.domain.vo.TicketStatsVO;
import com.tsd.csm.modules.stats.mapper.AgentWorkDailyMapper;
import com.tsd.csm.modules.ticket.domain.Ticket;
import com.tsd.csm.modules.ticket.mapper.TicketMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计分析服务（本租户）：工单维度实时统计 + 客服维度读日汇总表。
 */
@Service
public class StatsService extends ServiceImpl<AgentWorkDailyMapper, AgentWorkDaily> {

    private final TicketMapper ticketMapper;
    private final AccountMapper accountMapper;

    public StatsService(TicketMapper ticketMapper, AccountMapper accountMapper) {
        this.ticketMapper = ticketMapper;
        this.accountMapper = accountMapper;
    }

    public TicketStatsVO ticketStats(LocalDate start, LocalDate end) {
        LocalDateTime from = start == null ? null : start.atStartOfDay();
        LocalDateTime to = end == null ? null : end.plusDays(1).atStartOfDay();

        TicketStatsVO vo = new TicketStatsVO();
        vo.setTotal(countByStatus(from, to, null));
        vo.setQa(countByStatus(from, to, TicketStatus.QA.getCode()));
        vo.setTransferring(countByStatus(from, to, TicketStatus.TRANSFERRING.getCode()));
        vo.setProcessing(countByStatus(from, to, TicketStatus.PROCESSING.getCode()));
        long closed = countByStatus(from, to, TicketStatus.CLOSED.getCode());
        vo.setClosed(closed);
        vo.setCloseRate(vo.getTotal() == 0 ? 0d : round(closed * 1.0 / vo.getTotal()));
        vo.setAvgHandleSeconds(avgHandleSeconds(from, to));
        return vo;
    }

    public List<AgentStatVO> agentStats(LocalDate start, LocalDate end, Long agentId) {
        List<AgentWorkDaily> rows = lambdaQuery()
                .ge(start != null, AgentWorkDaily::getStatDate, start)
                .le(end != null, AgentWorkDaily::getStatDate, end)
                .eq(agentId != null, AgentWorkDaily::getAgentId, agentId)
                .list();

        Map<Long, AgentStatVO> grouped = new LinkedHashMap<>();
        Map<Long, Long> responseCostWeighted = new LinkedHashMap<>();
        for (AgentWorkDaily row : rows) {
            AgentStatVO vo = grouped.computeIfAbsent(row.getAgentId(), id -> {
                AgentStatVO created = new AgentStatVO();
                created.setAgentId(id);
                Account account = accountMapper.selectById(id);
                created.setRealName(account == null ? null : account.getRealName());
                return created;
            });
            vo.setOnlineSeconds(vo.getOnlineSeconds() + nz(row.getOnlineSeconds()));
            vo.setTicketCount(vo.getTicketCount() + nz(row.getTicketCount()));
            vo.setReplyCount(vo.getReplyCount() + nz(row.getReplyCount()));
            vo.setForceCloseCount(vo.getForceCloseCount() + nz(row.getForceCloseCount()));
            responseCostWeighted.merge(row.getAgentId(),
                    (long) nz(row.getAvgResponseCost()) * nz(row.getReplyCount()), Long::sum);
        }
        List<AgentStatVO> result = new ArrayList<>(grouped.values());
        for (AgentStatVO vo : result) {
            long weighted = responseCostWeighted.getOrDefault(vo.getAgentId(), 0L);
            vo.setAvgResponseCost(vo.getReplyCount() == 0 ? 0 : (int) (weighted / vo.getReplyCount()));
        }
        return result;
    }

    private long countByStatus(LocalDateTime from, LocalDateTime to, Integer status) {
        return ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                .ge(from != null, Ticket::getCreatedAt, from)
                .lt(to != null, Ticket::getCreatedAt, to)
                .eq(status != null, Ticket::getStatus, status));
    }

    private long avgHandleSeconds(LocalDateTime from, LocalDateTime to) {
        List<Ticket> closed = ticketMapper.selectList(new LambdaQueryWrapper<Ticket>()
                .ge(from != null, Ticket::getCreatedAt, from)
                .lt(to != null, Ticket::getCreatedAt, to)
                .eq(Ticket::getStatus, TicketStatus.CLOSED.getCode())
                .isNotNull(Ticket::getClosedAt));
        if (closed.isEmpty()) {
            return 0L;
        }
        long sum = 0L;
        for (Ticket ticket : closed) {
            sum += Duration.between(ticket.getCreatedAt(), ticket.getClosedAt()).getSeconds();
        }
        return sum / closed.size();
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return Math.round(value * 10000d) / 10000d;
    }
}
