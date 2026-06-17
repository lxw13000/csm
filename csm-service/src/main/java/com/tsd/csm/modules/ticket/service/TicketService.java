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
        if (ticket.getAgentId() != null
                && (status == TicketStatus.PROCESSING.getCode() || status == TicketStatus.TRANSFERRING.getCode())) {
            // 已分配客服（处理中，或已分配待接入）：实时推送给该客服
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

    /**
     * 用户标记已解决：完结其当前工单。
     * @param userId C 端用户 id
     * @return 完结后的工单
     */
    public Ticket resolveByUser(String userId) {
        Ticket ticket = activeOrThrow(userId);
        return closeTicket(ticket.getId(), CloseType.USER_RESOLVED.getCode());
    }

    /**
     * 客服强制完结工单。
     * @param ticketId 工单 id
     * @return 完结后的工单
     */
    public Ticket forceClose(Long ticketId) {
        return closeTicket(ticketId, CloseType.AGENT_FORCE.getCode());
    }

    /**
     * 超时自动完结工单（供定时任务调用）。
     * @param ticketId 工单 id
     * @return 完结后的工单
     */
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

    /**
     * 推进已读水位（校验工单归属后委托消息服务）。
     * @param ticketId 工单 id
     * @param readerType 阅读方类型：1 用户 / 2 客服
     * @param readerId 阅读方标识
     * @param seq 已读到的消息序号
     */
    public void markRead(Long ticketId, int readerType, String readerId, long seq) {
        getOwned(ticketId);
        messageService.markRead(ticketId, readerType, readerId, seq);
    }

    /**
     * 工单消息列表。
     * @param ticketId 工单 id
     * @param afterSeq 增量游标，可空
     * @return 消息 VO 列表
     */
    public List<MessageVO> messages(Long ticketId, Long afterSeq) {
        getOwned(ticketId);
        return messageService.history(ticketId, afterSeq).stream().map(messageService::toVO).toList();
    }

    /**
     * 工单最近 limit 条消息（初次加载取最新、向上滚动加载更早历史）。
     * @param ticketId 工单 id
     * @param beforeSeq 向上翻页游标：仅取 seq 小于它的消息，null 表示取最新
     * @param limit 返回条数上限，null 默认 10
     * @return 按 seq 升序的消息 VO 列表
     */
    public List<MessageVO> messagesBefore(Long ticketId, Long beforeSeq, Integer limit) {
        getOwned(ticketId);
        int size = (limit == null || limit <= 0) ? 10 : limit;
        return messageService.historyBefore(ticketId, beforeSeq, size).stream().map(messageService::toVO).toList();
    }

    /**
     * 管理端分页查询本租户工单。
     * @param query 查询条件
     * @return 工单分页结果
     */
    public PageResult<TicketVO> pageForAdmin(TicketQuery query) {
        Page<Ticket> page = lambdaQuery()
                .eq(StringUtils.hasText(query.getUserId()), Ticket::getUserId, query.getUserId())
                .eq(query.getStatus() != null, Ticket::getStatus, query.getStatus())
                .eq(query.getAgentId() != null, Ticket::getAgentId, query.getAgentId())
                .orderByDesc(Ticket::getId)
                .page(Page.of(query.getCurrent(), query.getSize()));
        return PageResult.of(page, ticket -> toVO(ticket, false, 0, null));
    }

    /** 客服「待我处理」会话：分配给我且未完结的工单（含待接入与处理中），带未读数。 */
    public List<TicketVO> listForAgent(Long agentId) {
        List<Ticket> tickets = lambdaQuery()
                .eq(Ticket::getAgentId, agentId)
                .ne(Ticket::getStatus, TicketStatus.CLOSED.getCode())
                .orderByDesc(Ticket::getLastMsgAt)
                .list();
        return tickets.stream()
                .map(ticket -> toVO(ticket, true, ReaderType.AGENT.getCode(), String.valueOf(agentId)))
                .toList();
    }

    /**
     * 客服查看工单详情（含未读数）。
     * @param ticketId 工单 id
     * @param agentId 客服账号 id
     * @return 工单详情
     */
    public TicketVO detailForAgent(Long ticketId, Long agentId) {
        Ticket ticket = getOwned(ticketId);
        return toVO(ticket, true, ReaderType.AGENT.getCode(), String.valueOf(agentId));
    }

    /**
     * 客服点开工单：将分配给该客服且处于「人工转接中」的工单转为「处理中」，即真正「接入人工」
     * （派单仅代表归属，点开才代表介入，xuqiu.md 4.2）。会通知用户「人工已接入」。
     * @param ticketId 工单 id
     * @param agentId 客服账号 id
     * @return 接入后的工单详情
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketVO acceptTicket(Long ticketId, Long agentId) {
        Ticket ticket = getOwned(ticketId);
        if (agentId != null && agentId.equals(ticket.getAgentId())
                && ticket.getStatus() == TicketStatus.TRANSFERRING.getCode()) {
            ticket.setStatus(TicketStatus.PROCESSING.getCode());
            if (ticket.getAssignedAt() == null) {
                ticket.setAssignedAt(LocalDateTime.now());
            }
            updateById(ticket);
            notifyTicketStatus(ticket, agentId);
        }
        return detailForAgent(ticketId, agentId);
    }

    /**
     * 用户当前工单（无则新建并进入智能问答）。
     * @param userId C 端用户 id
     * @return 当前工单详情
     */
    public TicketVO currentForUser(String userId) {
        Ticket ticket = getOrCreateActive(userId);
        return toVO(ticket, true, ReaderType.USER.getCode(), userId);
    }

    /** 取用户当前未完结工单，没有则抛业务异常。 */
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

    /** 取用户最近一条工单（含已完结），无则返回 null。 */
    private Ticket activeOrLast(String userId) {
        return lambdaQuery()
                .eq(Ticket::getUserId, userId)
                .orderByDesc(Ticket::getId)
                .last("limit 1")
                .one();
    }

    /** 按 id 取本租户工单，不存在抛 404。 */
    private Ticket getOwned(Long id) {
        Ticket ticket = getById(id);
        if (ticket == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return ticket;
    }

    /** 向用户（及当前客服）推送工单状态变更。 */
    private void notifyTicketStatus(Ticket ticket, Long agentId) {
        Map<String, Object> payload = statusPayload(ticket);
        notifier.toUser(ticket.getAppId(), ticket.getUserId(), WsChannelType.TICKET_STATUS.getType(), payload);
        if (agentId != null) {
            notifier.toAgent(ticket.getAppId(), agentId, WsChannelType.TICKET_STATUS.getType(), payload);
        }
    }

    /** 组装工单状态变更推送载荷。 */
    private Map<String, Object> statusPayload(Ticket ticket) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticketId", ticket.getId());
        payload.put("status", ticket.getStatus());
        payload.put("closeType", ticket.getCloseType());
        payload.put("userId", ticket.getUserId());
        return payload;
    }

    /**
     * 工单实体转展示 VO，可选附带客户资料与未读数。
     * @param ticket 工单实体
     * @param withUnread 是否计算未读数
     * @param readerType 阅读方类型（计算未读数时用）
     * @param readerId 阅读方标识（计算未读数时用）
     * @return 工单 VO
     */
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
