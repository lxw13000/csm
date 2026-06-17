package com.tsd.csm.modules.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.ticket.domain.MessageRead;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息已读水位 Mapper。对应 csm_message_read。
 */
@Mapper
public interface MessageReadMapper extends BaseMapper<MessageRead> {
}
