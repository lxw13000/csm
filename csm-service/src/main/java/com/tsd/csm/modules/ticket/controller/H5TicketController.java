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

    /**
     * 当前进行中工单（无则新建并进入智能问答阶段）。
     * @return 当前工单
     */
    @GetMapping("/ticket/current")
    public R<TicketVO> current() {
        return R.ok(ticketService.currentForUser(currentUserId()));
    }

    /**
     * 工单聊天记录。
     * @param ticketId 工单 id
     * @param afterSeq 增量游标，可空
     * @return 消息列表
     */
    @GetMapping("/ticket/messages")
    public R<List<MessageVO>> messages(@RequestParam Long ticketId,
                                       @RequestParam(required = false) Long afterSeq) {
        return R.ok(ticketService.messages(ticketId, afterSeq));
    }

    /**
     * 用户发消息（按工单状态走智能问答 / 转推客服 / 转人工）。
     * @param dto 消息内容
     * @return 处理结果（含机器人回复或转人工标记）
     */
    @PostMapping("/message")
    public R<UserMessageResultVO> sendMessage(@RequestBody @Valid SendMessageDTO dto) {
        return R.ok(ticketService.handleUserMessage(currentUserId(), dto));
    }

    /**
     * 转人工（取当前工单并触发派单）。
     * @return 工单
     */
    @PostMapping("/transfer")
    public R<Ticket> transfer() {
        Ticket active = ticketService.getOrCreateActive(currentUserId());
        return R.ok(ticketService.requestHuman(active.getId()));
    }

    /**
     * 用户标记「已解决」并完结工单。
     * @return 完结后的工单
     */
    @PostMapping("/resolve")
    public R<Ticket> resolve() {
        return R.ok(ticketService.resolveByUser(currentUserId()));
    }

    /**
     * 用户标记「未解决」，保持会话继续。
     * @return 工单
     */
    @PostMapping("/unresolved")
    public R<Ticket> unresolved() {
        return R.ok(ticketService.continueTalk(currentUserId()));
    }

    /**
     * 提交服务评价（标记已解决时若工单未完结则一并完结）。
     * @param dto 评价内容
     */
    @PostMapping("/evaluate")
    public R<Void> evaluate(@RequestBody EvaluateDTO dto) {
        ticketService.evaluate(currentUserId(), dto);
        return R.ok();
    }

    /**
     * 上报用户已读水位。
     * @param ticketId 工单 id
     * @param seq 已读到的消息序号
     */
    @PostMapping("/ticket/read")
    public R<Void> read(@RequestParam Long ticketId, @RequestParam long seq) {
        ticketService.markRead(ticketId, ReaderType.USER.getCode(), currentUserId(), seq);
        return R.ok();
    }

    /** 取当前 C 端登录用户的 user_id。 */
    private String currentUserId() {
        LoginUser user = UserContext.required();
        return user.getUserId();
    }
}
