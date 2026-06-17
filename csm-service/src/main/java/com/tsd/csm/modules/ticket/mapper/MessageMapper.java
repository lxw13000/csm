package com.tsd.csm.modules.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.ticket.domain.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /** 当前工单内最大消息序号（租户条件由拦截器追加）。 */
    @Select("SELECT IFNULL(MAX(seq),0) FROM csm_message WHERE ticket_id = #{ticketId}")
    Long selectMaxSeq(@Param("ticketId") Long ticketId);
}
