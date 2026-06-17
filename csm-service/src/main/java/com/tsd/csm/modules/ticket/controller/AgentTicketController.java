package com.tsd.csm.modules.ticket.controller;

import com.tsd.csm.core.audit.AuditLog;
import com.tsd.csm.core.common.enums.ReaderType;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.LoginUser;
import com.tsd.csm.core.security.UserContext;
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

    public AgentTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/list")
    public R<List<TicketVO>> list() {
        return R.ok(ticketService.listForAgent(currentAgentId()));
    }

    @GetMapping("/{id}")
    public R<TicketVO> detail(@PathVariable Long id) {
        return R.ok(ticketService.detailForAgent(id, currentAgentId()));
    }

    @GetMapping("/{id}/messages")
    public R<List<MessageVO>> messages(@PathVariable Long id,
                                       @RequestParam(required = false) Long afterSeq) {
        return R.ok(ticketService.messages(id, afterSeq));
    }

    @PostMapping("/{id}/reply")
    public R<MessageVO> reply(@PathVariable Long id, @RequestBody @Valid SendMessageDTO dto) {
        return R.ok(ticketService.agentReply(id, currentAgentId(), dto));
    }

    @PostMapping("/{id}/transfer")
    @AuditLog(module = "ticket", action = "transfer", targetType = "ticket")
    public R<Ticket> transfer(@PathVariable Long id, @RequestBody @Valid TransferDTO dto) {
        return R.ok(ticketService.transfer(id, dto.getToAgentId(), dto.getReason()));
    }

    @PostMapping("/{id}/close")
    @AuditLog(module = "ticket", action = "close", targetType = "ticket")
    public R<Ticket> close(@PathVariable Long id) {
        return R.ok(ticketService.forceClose(id));
    }

    @PostMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id, @RequestParam long seq) {
        ticketService.markRead(id, ReaderType.AGENT.getCode(), String.valueOf(currentAgentId()), seq);
        return R.ok();
    }

    private Long currentAgentId() {
        LoginUser user = UserContext.required();
        return user.getAccountId();
    }
}
