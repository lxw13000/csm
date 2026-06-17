package com.tsd.csm.modules.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.qa.domain.Qa;
import org.apache.ibatis.annotations.Mapper;

/**
 * QA 问答对 Mapper。对应 csm_qa。
 */
@Mapper
public interface QaMapper extends BaseMapper<Qa> {
}
