package com.tsd.csm.modules.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsd.csm.modules.customer.domain.Customer;
import org.apache.ibatis.annotations.Mapper;

/**
 * C 端用户缓存 Mapper。对应 csm_customer。
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
