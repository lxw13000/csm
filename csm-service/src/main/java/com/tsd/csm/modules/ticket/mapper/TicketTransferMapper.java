package com.tsd.csm.modules.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.ticket.domain.TicketTransfer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单转接记录 Mapper。对应 csm_ticket_transfer。
 */
@Mapper
public interface TicketTransferMapper extends BaseMapper<TicketTransfer> {
}
