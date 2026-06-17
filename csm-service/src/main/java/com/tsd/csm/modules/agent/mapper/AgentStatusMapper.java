package com.tsd.csm.modules.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.agent.domain.AgentStatus;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentStatusMapper extends BaseMapper<AgentStatus> {
}
