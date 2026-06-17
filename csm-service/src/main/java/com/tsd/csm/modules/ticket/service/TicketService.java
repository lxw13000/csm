package com.tsd.csm.modules.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsd.csm.core.common.enums.CloseType;
import com.tsd.csm.core.common.enums.ReaderType;
import com.tsd.csm.core.common.enums.SenderType;
import com.tsd.csm.core.common.enums.TicketStatus;
import com.tsd.csm.core.common.enums.WsChannelType;
import com.tsd.csm.core.common.exception.BizException;
import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.core.common.result.ResultCode;
import com.tsd.csm.core.realtime.RealtimeNotifier;
import com.tsd.csm.modules.customer.domain.Customer;
import com.tsd.csm.modules.customer.service.CustomerService;
import com.tsd.csm.modules.qa.domain.Qa;
import com.tsd.csm.modules.qa.service.QaService;
import com.tsd.csm.modules.ticket.domain.Message;
import com.tsd.csm.modules.ticket.domain.Ticket;
import com.tsd.csm.modules.ticket.domain.TicketEvaluation;
import com.tsd.csm.modules.ticket.domain.TicketTransfer;
import com.tsd.csm.modules.ticket.domain.dto.EvaluateDTO;
import com.tsd.csm.modules.ticket.domain.dto.SendMessageDTO;
import com.tsd.csm.modules.ticket.domain.dto.TicketQuery;
import com.tsd.csm.modules.ticket.domain.vo.MessageVO;
import com.tsd.csm.modules.ticket.domain.vo.TicketVO;
import com.tsd.csm.modules.ticket.domain.vo.UserMessageResultVO;
import com.tsd.csm.modules.ticket.mapper.TicketEvaluationMapper;
import com.tsd.csm.modules.ticket.mapper.TicketMapper;
import com.tsd.csm.modules.ticket.mapper.TicketTransferMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单服务：生命周期（创建/转人工/完结/转接/评价）、智能问答编排、消息读写编排。
 */
@Service
public class TicketService extends ServiceImpl<TicketMapper, Ticket> {

    private final MessageService messageService;
    private final DispatchService dispatchService;
    private final QaService qaService;
    private final CustomerService customerService;
    private final TicketEvaluationMapper ticketEvaluationMapper;
    private final TicketTransferMapper ticketTransferMapper;
    private final RealtimeNotifier notifier;

    public TicketService(MessageService messageService, DispatchService dispatchService, QaService qaService,
                         CustomerService customerService, TicketEvaluationMapper ticketEvaluationMapper,
                         TicketTransferMapper ticketTransferMapper, RealtimeNotifier notifier) {
        this.messageService = messageService;
        this.dispatchService = dispatchService;
        this.qaService = qaService;
        this.customerService = customerService;
        this.ticketEvaluationMapper = ticketEvaluationMapper;
        this.ticketTransferMapper = ticketTransferMapper;
        this.notifier = notifier;
    }

    /** 取该用户当前未完结工单，否则新建（首次进入智能问答阶段）。 */
    public Ticket getOrCreateActive(String userId) {
        Ticket ticket = lambdaQuery()
                .eq(Ticket::getUserId, userId)
                .ne(Ticket::getStatus, TicketStatus.CLOSED.getCode())
                .orderByDesc(Ticket::getId)
                .last("limit 1")
                .one();
        if (ticket != null) {
            return ticket;
        }
        ticket = new Ticket();
        ticket.setUserId(userId);
        ticket.setStatus(TicketStatus.QA.getCode());
        save(ticket);
        return ticket;
    }

    /**
     * 处理 C 端用户消息：落库 → 按工单状态分流（处理中转推客服 / 智能问答阶段命中回机器人、未命中转人工）。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserMessageResultVO handleUserMessage(String userId, SendMessageDTO dto) {
        Ticket ticket = getOrCreateActive(userId);
        Message userMessage = messageService.saveMessage(ticket.getId(), SenderType.USER.getCode(), userId,
                dto.getContentType(), dto.getContent(), dto.getClientMsgId());

        UserMessageResultVO result = new UserMessageResultVO();
        result.setMessage(messageService.toVO(userMessage));

        int status = ticket.getStatus();
        if (status == TicketStatus.PROCESSING.getCode() && ticket.getAgentId() != null) {
            notifier.toAgent(ticket.getAppId(), ticket.getAgentId(),
                    WsChannelType.CHAT.getType(), messageService.toVO(userMessage));
        } else if (status == TicketStatus.QA.getCode()) {
            Qa qa = qaService.matchBest(dto.getContent());
            if (qa != null) {
                Message bot = messageService.saveMessage(ticket.getId(), SenderType.SYSTEM.getCode(), null,
                        1, qa.getAnswer(), null);
                result.setBotReply(messageService.toVO(bot));
                notifier.toUser(ticket.getAppId(), userId, WsChannelType.CHAT.getType(), messageService.toVO(bot));
            } else {
                requestHuman(ticket.getId());
                result.setTransferred(true);
            }
        }
        result.setTicket(toVO(getById(ticket.getId()), false, 0, null));
        return result;
    }

    /** 转人工：QA/排队阶段置为「人工转接中」并触发派单。 */
    public Ticket requestHuman(Long ticketId) {
        Ticket ticket = getOwned(ticketId);
        if (ticket.getStatus() == TicketStatus.CLOSED.getCode()) {
            throw new BizException("工单已完结");
        }
        if (ticket.getStatus() != TicketStatus.PROCESSING.getCode()) {
            ticket.setStatus(TicketStatus.TRANSFERRING.getCode());
            updateById(ticket);
            dispatchService.dispatch(ticketId);
        }
        return getById(ticketId);
    }

    /** 客服回复：落库并实时推送给用户。 */
    public MessageVO agentReply(Long ticketId, Long agentId, SendMessageDTO dto) {
        Ticket ticket = getOwned(ticketId);
        if (ticket.getStatus() == TicketStatus.CLOSED.getCode()) {
            throw new BizException("工单已完结，无法回复");
        }
        Message message = messageService.saveMessage(ticketId, SenderType.AGENT.getCode(),
                String.valueOf(agentId), dto.getContentType(), dto.getContent(), dto.getClientMsgId());
        notifier.toUser(ticket.getAppId(), ticket.getUserId(),
                WsChannelType.CHAT.getType(), messageService.toVO(message));
        return messageService.toVO(message);
    }

    /** 工单完结：释放客服负载、重派队列工单、双向推送状态变更。 */
    @Transactional(rollbackFor = Exception.class)
    public Ticket closeTicket(Long ticketId, int closeType) {
        Ticket ticket = getOwned(ticketId);
        if (ticket.getStatus() == TicketStatus.CLOSED.getCode()) {
            return ticket;
        }
        Long agentId = ticket.getAgentId();
        ticket.setStatus(TicketStatus.CLOSED.getCode());
        ticket.setCloseType(closeType);
        ticket.setClosedAt(LocalDateTime.now());
        updateById(ticket);

        if (agentId != null) {
            dispatchService.releaseLoad(agentId);
        }
        notifyTicketStatus(ticket, agentId);
        if (agentId != null) {
            dispatchService.dispatchQueued();
        }
        return ticket;
    }

    public Ticket resolveByUser(String userId) {
        Ticket ticket = activeOrThrow(userId);
        return closeTicket(ticket.getId(), CloseType.USER_RESOLVED.getCode());
    }

    public Ticket forceClose(Long ticketId) {
        return closeTicket(ticketId, CloseType.AGENT_FORCE.getCode());
    }

    public Ticket autoClose(Long ticketId) {
        return closeTicket(ticketId, CloseType.AUTO_TIMEOUT.getCode());
    }

    /** 用户点击「未解决」继续交流：工单保持当前状态。 */
    public Ticket continueTalk(String userId) {
        return activeOrThrow(userId);
    }

    /** 工单转接给其他客服。 */
    @Transactional(rollbackFor = Exception.class)
    public Ticket transfer(Long ticketId, Long toAgentId, String reason) {
        Ticket ticket = getOwned(ticketId);
        if (ticket.getStatus() == TicketStatus.CLOSED.getCode()) {
            throw new BizException("工单已完结，无法转接");
        }
        Long fromAgentId = ticket.getAgentId();

        TicketTransfer transfer = new TicketTransfer();
        transfer.setTicketId(ticketId);
        transfer.setFromAgentId(fromAgentId);
        transfer.setToAgentId(toAgentId);
        transfer.setReason(reason);
        ticketTransferMapper.insert(transfer);

        if (fromAgentId != null) {
            dispatchService.releaseLoad(fromAgentId);
        }
        dispatchService.incrementLoad(toAgentId);
        ticket.setAgentId(toAgentId);
        ticket.setStatus(TicketStatus.PROCESSING.getCode());
        updateById(ticket);

        Map<String, Object> payload = statusPayload(ticket);
        notifier.toAgent(ticket.getAppId(), toAgentId, WsChannelType.NOTIFICATION.getType(), payload);
        notifier.toAgent(ticket.getAppId(), toAgentId, WsChannelType.TICKET_STATUS.getType(), payload);
        notifier.toUser(ticket.getAppId(), ticket.getUserId(), WsChannelType.TICKET_STATUS.getType(), payload);
        return ticket;
    }

    /** 服务评价（每工单一条）；标记已解决时若工单未完结则一并完结。 */
    @Transactional(rollbackFor = Exception.class)
    public void evaluate(String userId, EvaluateDTO dto) {
        Ticket ticket = activeOrLast(userId);
        if (ticket == null) {
            throw new BizException("暂无可评价的工单");
        }
        TicketEvaluation existing = ticketEvaluationMapper.selectOne(
                new LambdaQueryWrapper<TicketEvaluation>().eq(TicketEvaluation::getTicketId, ticket.getId()));
        TicketEvaluation evaluation = existing == null ? new TicketEvaluation() : existing;
        evaluation.setTicketId(ticket.getId());
        evaluation.setResolved(dto.getResolved());
        evaluation.setRating(dto.getRating());
        evaluation.setRemark(dto.getRemark());
        if (existing == null) {
            ticketEvaluationMapper.insert(evaluation);
        } else {
            ticketEvaluationMapper.updateById(evaluation);
        }
        if (dto.getResolved() != null && dto.getResolved() == 1
                && ticket.getStatus() != TicketStatus.CLOSED.getCode()) {
            closeTicket(ticket.getId(), CloseType.USER_RESOLVED.getCode());
        }
    }

    public void markRead(Long ticketId, int readerType, String readerId, long seq) {
        getOwned(ticketId);
        messageService.markRead(ticketId, readerType, readerId, seq);
    }

    public List<MessageVO> messages(Long ticketId, Long afterSeq) {
        getOwned(ticketId);
        return messageService.history(ticketId, afterSeq).stream().map(messageService::toVO).toList();
    }

    public PageResult<TicketVO> pageForAdmin(TicketQuery query) {
        Page<Ticket> page = lambdaQuery()
                .eq(StringUtils.hasText(query.getUserId()), Ticket::getUserId, query.getUserId())
                .eq(query.getStatus() != null, Ticket::getStatus, query.getStatus())
                .eq(query.getAgentId() != null, Ticket::getAgentId, query.getAgentId())
                .orderByDesc(Ticket::getId)
                .page(Page.of(query.getCurrent(), query.getSize()));
        return PageResult.of(page, ticket -> toVO(ticket, false, 0, null));
    }

    /** 客服「我的」会话：处理中工单，带未读数。 */
    public List<TicketVO> listForAgent(Long agentId) {
        List<Ticket> tickets = lambdaQuery()
                .eq(Ticket::getAgentId, agentId)
                .eq(Ticket::getStatus, TicketStatus.PROCESSING.getCode())
                .orderByDesc(Ticket::getLastMsgAt)
                .list();
        return tickets.stream()
                .map(ticket -> toVO(ticket, true, ReaderType.AGENT.getCode(), String.valueOf(agentId)))
                .toList();
    }

    public TicketVO detailForAgent(Long ticketId, Long agentId) {
        Ticket ticket = getOwned(ticketId);
        return toVO(ticket, true, ReaderType.AGENT.getCode(), String.valueOf(agentId));
    }

    public TicketVO currentForUser(String userId) {
        Ticket ticket = getOrCreateActive(userId);
        return toVO(ticket, true, ReaderType.USER.getCode(), userId);
    }

    private Ticket activeOrThrow(String userId) {
        Ticket ticket = lambdaQuery()
                .eq(Ticket::getUserId, userId)
                .ne(Ticket::getStatus, TicketStatus.CLOSED.getCode())
                .orderByDesc(Ticket::getId)
                .last("limit 1")
                .one();
        if (ticket == null) {
            throw new BizException("当前没有进行中的工单");
        }
        return ticket;
    }

    private Ticket activeOrLast(String userId) {
        return lambdaQuery()
                .eq(Ticket::getUserId, userId)
                .orderByDesc(Ticket::getId)
                .last("limit 1")
                .one();
    }

    private Ticket getOwned(Long id) {
        Ticket ticket = getById(id);
        if (ticket == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return ticket;
    }

    private void notifyTicketStatus(Ticket ticket, Long agentId) {
        Map<String, Object> payload = statusPayload(ticket);
        notifier.toUser(ticket.getAppId(), ticket.getUserId(), WsChannelType.TICKET_STATUS.getType(), payload);
        if (agentId != null) {
            notifier.toAgent(ticket.getAppId(), agentId, WsChannelType.TICKET_STATUS.getType(), payload);
        }
    }

    private Map<String, Object> statusPayload(Ticket ticket) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticketId", ticket.getId());
        payload.put("status", ticket.getStatus());
        payload.put("closeType", ticket.getCloseType());
        payload.put("userId", ticket.getUserId());
        return payload;
    }

    private TicketVO toVO(Ticket ticket, boolean withUnread, int readerType, String readerId) {
        TicketVO vo = new TicketVO();
        vo.setId(ticket.getId());
        vo.setUserId(ticket.getUserId());
        vo.setStatus(ticket.getStatus());
        vo.setCloseType(ticket.getCloseType());
        vo.setAgentId(ticket.getAgentId());
        vo.setFirstMsgAt(ticket.getFirstMsgAt());
        vo.setLastMsgAt(ticket.getLastMsgAt());
        vo.setClosedAt(ticket.getClosedAt());
        vo.setCreatedAt(ticket.getCreatedAt());
        Customer customer = customerService.getCached(ticket.getUserId());
        if (customer != null) {
            vo.setNickname(customer.getNickname());
            vo.setAvatar(customer.getAvatar());
        }
        if (withUnread && readerId != null) {
            vo.setUnreadCount(messageService.unreadCount(ticket.getId(), readerType, readerId));
        }
        return vo;
    }
}
