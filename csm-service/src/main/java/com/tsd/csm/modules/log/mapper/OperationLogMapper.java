package com.tsd.csm.modules.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.log.domain.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计日志 Mapper。对应 csm_operation_log。
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
