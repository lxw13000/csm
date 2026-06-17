package com.tsd.csm.modules.stats.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.stats.domain.AgentWorkDaily;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客服工作日汇总 Mapper。对应 csm_agent_work_daily。
 */
@Mapper
public interface AgentWorkDailyMapper extends BaseMapper<AgentWorkDaily> {
}
