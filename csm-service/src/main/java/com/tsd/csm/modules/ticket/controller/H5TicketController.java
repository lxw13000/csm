package com.tsd.csm.modules.ticket.controller;

import com.tsd.csm.core.common.enums.ReaderType;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.LoginUser;
import com.tsd.csm.core.security.UserContext;
import com.tsd.csm.modules.ticket.domain.Ticket;
import com.tsd.csm.modules.ticket.domain.dto.EvaluateDTO;
import com.tsd.csm.modules.ticket.domain.dto.SendMessageDTO;
import com.tsd.csm.modules.ticket.domain.vo.MessageVO;
import com.tsd.csm.modules.ticket.domain.vo.TicketVO;
import com.tsd.csm.modules.ticket.domain.vo.UserMessageResultVO;
import com.tsd.csm.modules.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 H5 工单与会话：当前工单、发消息（智能问答/转人工）、聊天记录、转人工、已解决/未解决、评价、已读。
 * 身份来自会话凭证（session token），由 AuthInterceptor 解析为 C 端用户上下文。
 */
@RestController
@RequestMapping("/api/h5")
public class H5TicketController {

    private final TicketService ticketService;

    public H5TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/ticket/current")
    public R<TicketVO> current() {
        return R.ok(ticketService.currentForUser(currentUserId()));
    }

    @GetMapping("/ticket/messages")
    public R<List<MessageVO>> messages(@RequestParam Long ticketId,
                                       @RequestParam(required = false) Long afterSeq) {
        return R.ok(ticketService.messages(ticketId, afterSeq));
    }

    @PostMapping("/message")
    public R<UserMessageResultVO> sendMessage(@RequestBody @Valid SendMessageDTO dto) {
        return R.ok(ticketService.handleUserMessage(currentUserId(), dto));
    }

    @PostMapping("/transfer")
    public R<Ticket> transfer() {
        Ticket active = ticketService.getOrCreateActive(currentUserId());
        return R.ok(ticketService.requestHuman(active.getId()));
    }

    @PostMapping("/resolve")
    public R<Ticket> resolve() {
        return R.ok(ticketService.resolveByUser(currentUserId()));
    }

    @PostMapping("/unresolved")
    public R<Ticket> unresolved() {
        return R.ok(ticketService.continueTalk(currentUserId()));
    }

    @PostMapping("/evaluate")
    public R<Void> evaluate(@RequestBody EvaluateDTO dto) {
        ticketService.evaluate(currentUserId(), dto);
        return R.ok();
    }

    @PostMapping("/ticket/read")
    public R<Void> read(@RequestParam Long ticketId, @RequestParam long seq) {
        ticketService.markRead(ticketId, ReaderType.USER.getCode(), currentUserId(), seq);
        return R.ok();
    }

    private String currentUserId() {
        LoginUser user = UserContext.required();
        return user.getUserId();
    }
}
