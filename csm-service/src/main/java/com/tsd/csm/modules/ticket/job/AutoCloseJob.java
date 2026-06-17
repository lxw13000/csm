package com.tsd.csm.modules.ticket.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tsd.csm.core.common.enums.TicketStatus;
import com.tsd.csm.core.tenant.TenantContext;
import com.tsd.csm.modules.config.domain.TenantConfig;
import com.tsd.csm.modules.config.service.TenantConfigService;
import com.tsd.csm.modules.ticket.domain.Ticket;
import com.tsd.csm.modules.ticket.mapper.TicketMapper;
import com.tsd.csm.modules.ticket.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自动完结定时任务：用户无操作超过租户配置的倒计时（默认 15 分钟）则自动完结（close_type=2）。
 * 跨租户扫描用 executeIgnore，按租户的阈值判定并在各自上下文中完结。
 */
@Component
public class AutoCloseJob {

    private static final Logger log = LoggerFactory.getLogger(AutoCloseJob.class);

    private final TicketMapper ticketMapper;
    private final TenantConfigService tenantConfigService;
    private final TicketService ticketService;

    public AutoCloseJob(TicketMapper ticketMapper, TenantConfigService tenantConfigService,
                        TicketService ticketService) {
        this.ticketMapper = ticketMapper;
        this.tenantConfigService = tenantConfigService;
        this.ticketService = ticketService;
    }

    /**
     * 每分钟扫描一次：跨租户取未完结且已有消息的工单，按各租户的超时阈值完结过期会话。
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    public void run() {
        List<Ticket> active = TenantContext.executeIgnore(() -> ticketMapper.selectList(
                new LambdaQueryWrapper<Ticket>()
                        .ne(Ticket::getStatus, TicketStatus.CLOSED.getCode())
                        .isNotNull(Ticket::getLastMsgAt)));
        if (active.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<Ticket>> byApp = active.stream()
                .collect(Collectors.groupingBy(Ticket::getAppId));
        byApp.forEach((appId, tickets) -> {
            TenantConfig config = TenantContext.callWithAppId(appId, tenantConfigService::getCurrent);
            int minutes = config.getAutoCloseMinutes() == null ? 15 : config.getAutoCloseMinutes();
            LocalDateTime threshold = now.minusMinutes(minutes);
            for (Ticket ticket : tickets) {
                if (ticket.getLastMsgAt() != null && ticket.getLastMsgAt().isBefore(threshold)) {
                    TenantContext.runWithAppId(appId, () -> {
                        ticketService.autoClose(ticket.getId());
                        log.debug("自动完结工单 app={} ticket={}", appId, ticket.getId());
                    });
                }
            }
        });
    }
}
