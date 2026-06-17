package com.tsd.csm.modules.ticket.controller;

import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.RequiresPermission;
import com.tsd.csm.modules.ticket.domain.dto.TicketQuery;
import com.tsd.csm.modules.ticket.domain.vo.MessageVO;
import com.tsd.csm.modules.ticket.domain.vo.TicketVO;
import com.tsd.csm.modules.ticket.service.TicketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单列表（PC 管理端，本租户）：会话组列表与聊天详情查看。
 */
@RestController
@RequestMapping("/api/admin/ticket")
public class AdminTicketController {

    private final TicketService ticketService;

    public AdminTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/page")
    @RequiresPermission("ticket:list")
    public R<PageResult<TicketVO>> page(TicketQuery query) {
        return R.ok(ticketService.pageForAdmin(query));
    }

    @GetMapping("/{id}/messages")
    @RequiresPermission("ticket:list")
    public R<List<MessageVO>> messages(@PathVariable Long id,
                                       @RequestParam(required = false) Long afterSeq) {
        return R.ok(ticketService.messages(id, afterSeq));
    }
}
