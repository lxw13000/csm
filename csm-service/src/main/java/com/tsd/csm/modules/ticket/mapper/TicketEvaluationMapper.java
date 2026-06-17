package com.tsd.csm.modules.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.ticket.domain.TicketEvaluation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单评价 Mapper。对应 csm_ticket_evaluation。
 */
@Mapper
public interface TicketEvaluationMapper extends BaseMapper<TicketEvaluation> {
}
