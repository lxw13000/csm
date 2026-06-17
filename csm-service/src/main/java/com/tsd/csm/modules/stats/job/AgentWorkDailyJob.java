package com.tsd.csm.modules.stats.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tsd.csm.core.common.enums.AccountType;
import com.tsd.csm.core.common.enums.CloseType;
import com.tsd.csm.core.common.enums.SenderType;
import com.tsd.csm.core.tenant.TenantContext;
import com.tsd.csm.modules.account.domain.Account;
import com.tsd.csm.modules.account.mapper.AccountMapper;
import com.tsd.csm.modules.stats.domain.AgentWorkDaily;
import com.tsd.csm.modules.stats.mapper.AgentWorkDailyMapper;
import com.tsd.csm.modules.ticket.domain.Message;
import com.tsd.csm.modules.ticket.domain.Ticket;
import com.tsd.csm.modules.ticket.mapper.MessageMapper;
import com.tsd.csm.modules.ticket.mapper.TicketMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客服工作情况日汇总：每日凌晨预聚合前一日 csm_ticket / csm_message 写入 csm_agent_work_daily，
 * 报表查询直接读汇总表（呼应 4.3.2）。online_seconds 暂未采集在线时长明细，置 0。
 */
@Component
public class AgentWorkDailyJob {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkDailyJob.class);

    private final AccountMapper accountMapper;
    private final TicketMapper ticketMapper;
    private final MessageMapper messageMapper;
    private final AgentWorkDailyMapper agentWorkDailyMapper;

    public AgentWorkDailyJob(AccountMapper accountMapper, TicketMapper ticketMapper,
                             MessageMapper messageMapper, AgentWorkDailyMapper agentWorkDailyMapper) {
        this.accountMapper = accountMapper;
        this.ticketMapper = ticketMapper;
        this.messageMapper = messageMapper;
        this.agentWorkDailyMapper = agentWorkDailyMapper;
    }

    /** 每日 0:10 触发，汇总前一日数据。 */
    @Scheduled(cron = "0 10 0 * * ?")
    public void aggregateYesterday() {
        aggregate(LocalDate.now().minusDays(1));
    }

    /** 聚合指定日期（可手动调用做补算/自测）。 */
    public void aggregate(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        List<Account> agents = TenantContext.executeIgnore(() -> accountMapper.selectList(
                new LambdaQueryWrapper<Account>().eq(Account::getAccountType, AccountType.AGENT.getCode())));

        for (Account agent : agents) {
            String appId = agent.getAppId();
            Long agentId = agent.getId();
            TenantContext.runWithAppId(appId, () -> aggregateOne(appId, agentId, date, dayStart, dayEnd));
        }
        log.info("客服工作日汇总完成 date={} agents={}", date, agents.size());
    }

    /** 聚合单个客服某日的接待量/回复数/平均响应/强制关闭数（全为 0 则不落库）。 */
    private void aggregateOne(String appId, Long agentId, LocalDate date,
                              LocalDateTime dayStart, LocalDateTime dayEnd) {
        long ticketCount = ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getAgentId, agentId)
                .ge(Ticket::getAssignedAt, dayStart)
                .lt(Ticket::getAssignedAt, dayEnd));

        List<Message> replies = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getSenderType, SenderType.AGENT.getCode())
                .eq(Message::getSenderId, String.valueOf(agentId))
                .ge(Message::getCreatedAt, dayStart)
                .lt(Message::getCreatedAt, dayEnd));
        int replyCount = replies.size();
        int avgResponseCost = averageResponseCost(replies);

        long forceCloseCount = ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getAgentId, agentId)
                .eq(Ticket::getCloseType, CloseType.AGENT_FORCE.getCode())
                .ge(Ticket::getClosedAt, dayStart)
                .lt(Ticket::getClosedAt, dayEnd));

        if (ticketCount == 0 && replyCount == 0 && forceCloseCount == 0) {
            return;
        }

        AgentWorkDaily existing = agentWorkDailyMapper.selectOne(new LambdaQueryWrapper<AgentWorkDaily>()
                .eq(AgentWorkDaily::getAgentId, agentId)
                .eq(AgentWorkDaily::getStatDate, date));
        AgentWorkDaily record = existing == null ? new AgentWorkDaily() : existing;
        record.setAgentId(agentId);
        record.setStatDate(date);
        record.setOnlineSeconds(0);
        record.setTicketCount((int) ticketCount);
        record.setReplyCount(replyCount);
        record.setAvgResponseCost(avgResponseCost);
        record.setForceCloseCount((int) forceCloseCount);
        if (existing == null) {
            agentWorkDailyMapper.insert(record);
        } else {
            agentWorkDailyMapper.updateById(record);
        }
    }

    /** 计算回复消息的平均响应耗时（秒，忽略无耗时记录）。 */
    private int averageResponseCost(List<Message> replies) {
        int sum = 0;
        int count = 0;
        for (Message reply : replies) {
            if (reply.getResponseCost() != null) {
                sum += reply.getResponseCost();
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }
}
