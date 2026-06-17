package com.tsd.csm.modules.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.ticket.domain.Ticket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单 Mapper。对应 csm_ticket。
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
}
