package com.tsd.csm.modules.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.qa.domain.QaKeyword;
import org.apache.ibatis.annotations.Mapper;

/**
 * QA 关键词关联 Mapper。对应 csm_qa_keyword。
 */
@Mapper
public interface QaKeywordMapper extends BaseMapper<QaKeyword> {
}
