package com.tsd.csm.modules.ticket.controller;

import com.tsd.csm.core.audit.AuditLog;
import com.tsd.csm.core.common.enums.ReaderType;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.LoginUser;
import com.tsd.csm.core.security.UserContext;
import com.tsd.csm.modules.account.domain.vo.AccountVO;
import com.tsd.csm.modules.account.service.AccountService;
import com.tsd.csm.modules.ticket.domain.Ticket;
import com.tsd.csm.modules.ticket.domain.dto.SendMessageDTO;
import com.tsd.csm.modules.ticket.domain.dto.TransferDTO;
import com.tsd.csm.modules.ticket.domain.vo.MessageVO;
import com.tsd.csm.modules.ticket.domain.vo.TicketVO;
import com.tsd.csm.modules.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客服端 H5 工单处理：会话列表、聊天记录、回复、转接、强制关闭、已读上报。
 */
@RestController
@RequestMapping("/api/agent/ticket")
public class AgentTicketController {

    private final TicketService ticketService;
    private final AccountService accountService;

    public AgentTicketController(TicketService ticketService, AccountService accountService) {
        this.ticketService = ticketService;
        this.accountService = accountService;
    }

    /**
     * 客服「我的」会话列表（处理中工单，带未读数）。
     * @return 工单列表
     */
    @GetMapping("/list")
    public R<List<TicketVO>> list() {
        return R.ok(ticketService.listForAgent(currentAgentId()));
    }

    /**
     * 工单详情（含未读数）。
     * @param id 工单 id
     * @return 工单详情
     */
    @GetMapping("/{id}")
    public R<TicketVO> detail(@PathVariable Long id) {
        return R.ok(ticketService.detailForAgent(id, currentAgentId()));
    }

    /**
     * 客服点开工单 = 接入人工：将分配给我的「人工转接中」工单转为「处理中」。
     * @param id 工单 id
     * @return 接入后的工单详情
     */
    @PostMapping("/{id}/accept")
    public R<TicketVO> accept(@PathVariable Long id) {
        return R.ok(ticketService.acceptTicket(id, currentAgentId()));
    }

    /**
     * 可转接的本租户客服列表（转接选择用）。
     * @return 客服账号列表
     */
    @GetMapping("/transfer-targets")
    public R<List<AccountVO>> transferTargets() {
        return R.ok(accountService.listAgents());
    }

    /**
     * 工单聊天记录。
     * @param id 工单 id
     * @param afterSeq 增量游标，可空
     * @return 消息列表
     */
    @GetMapping("/{id}/messages")
    public R<List<MessageVO>> messages(@PathVariable Long id,
                                       @RequestParam(required = false) Long afterSeq) {
        return R.ok(ticketService.messages(id, afterSeq));
    }

    /**
     * 客服回复消息（落库并实时推送给用户）。
     * @param id 工单 id
     * @param dto 消息内容
     * @return 落库后的消息
     */
    @PostMapping("/{id}/reply")
    public R<MessageVO> reply(@PathVariable Long id, @RequestBody @Valid SendMessageDTO dto) {
        return R.ok(ticketService.agentReply(id, currentAgentId(), dto));
    }

    /**
     * 转接工单给其他客服。
     * @param id 工单 id
     * @param dto 目标客服与转接原因
     * @return 转接后的工单
     */
    @PostMapping("/{id}/transfer")
    @AuditLog(module = "ticket", action = "transfer", targetType = "ticket")
    public R<Ticket> transfer(@PathVariable Long id, @RequestBody @Valid TransferDTO dto) {
        return R.ok(ticketService.transfer(id, dto.getToAgentId(), dto.getReason()));
    }

    /**
     * 客服强制关闭工单。
     * @param id 工单 id
     * @return 完结后的工单
     */
    @PostMapping("/{id}/close")
    @AuditLog(module = "ticket", action = "close", targetType = "ticket")
    public R<Ticket> close(@PathVariable Long id) {
        return R.ok(ticketService.forceClose(id));
    }

    /**
     * 上报客服已读水位。
     * @param id 工单 id
     * @param seq 已读到的消息序号
     */
    @PostMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id, @RequestParam long seq) {
        ticketService.markRead(id, ReaderType.AGENT.getCode(), String.valueOf(currentAgentId()), seq);
        return R.ok();
    }

    /** 取当前登录客服的账号 id。 */
    private Long currentAgentId() {
        LoginUser user = UserContext.required();
        return user.getAccountId();
    }
}
