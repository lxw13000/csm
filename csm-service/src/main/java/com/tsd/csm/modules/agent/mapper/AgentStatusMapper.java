package com.tsd.csm.modules.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.agent.domain.AgentStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客服在线状态 Mapper。对应 csm_agent_status。
 */
@Mapper
public interface AgentStatusMapper extends BaseMapper<AgentStatus> {
}
