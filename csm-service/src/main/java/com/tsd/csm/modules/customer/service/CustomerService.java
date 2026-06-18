package com.tsd.csm.modules.customer.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.modules.customer.domain.Customer;
import com.tsd.csm.modules.customer.domain.vo.CustomerVO;
import com.tsd.csm.modules.customer.mapper.CustomerMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * C 端用户缓存服务：业务系统换取通信凭证时同步昵称/头像（呼应 xuqiu.md 2.5）。
 */
@Service
public class CustomerService extends ServiceImpl<CustomerMapper, Customer> {

    /** 取用户缓存记录（无则返回 null）。 */
    public Customer getCached(String userId) {
        return lambdaQuery().eq(Customer::getUserId, userId).one();
    }

    /**
     * 业务系统换取凭证时同步缓存：按 user_id 插入或更新昵称/头像。
     * 要求 TenantContext.appId 已设置为该租户。
     * @param userId 业务系统用户 id
     * @param nickname 昵称（可空，空则不覆盖）
     * @param avatar 头像 URL（可空，空则不覆盖）
     * @return 缓存后的用户记录
     */
    public Customer upsert(String userId, String nickname, String avatar) {
        Customer c = getCached(userId);
        if (c == null) {
            c = new Customer();
            c.setUserId(userId);
        }
        if (StringUtils.hasText(nickname)) {
            c.setNickname(nickname);
        }
        if (StringUtils.hasText(avatar)) {
            c.setAvatar(avatar);
        }
        c.setLastSyncAt(LocalDateTime.now());
        saveOrUpdate(c);
        return c;
    }

    /** 用户详情：直接返回缓存（权威源为业务系统，昵称/头像随换取凭证同步）。 */
    public CustomerVO detail(String userId) {
        Customer cached = getCached(userId);
        if (cached == null) {
            CustomerVO vo = new CustomerVO();
            vo.setUserId(userId);
            return vo;
        }
        return toVO(cached);
    }

    /**
     * 分页检索本租户用户缓存。
     * @param current 页码
     * @param size 每页条数
     * @param keyword user_id/昵称模糊词，可空
     * @return 用户分页结果
     */
    public PageResult<CustomerVO> page(long current, long size, String keyword) {
        Page<Customer> page = lambdaQuery()
                // OR 条件必须整体分组，避免与拦截器追加的 app_id 条件因 AND/OR 优先级而越权
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Customer::getUserId, keyword)
                        .or().like(Customer::getNickname, keyword))
                .orderByDesc(Customer::getId)
                .page(Page.of(current, size));
        return PageResult.of(page, this::toVO);
    }

    /**
     * 缓存实体转展示 VO。
     * @param c 用户缓存实体
     * @return 用户 VO
     */
    public CustomerVO toVO(Customer c) {
        CustomerVO vo = new CustomerVO();
        vo.setUserId(c.getUserId());
        vo.setNickname(c.getNickname());
        vo.setAvatar(c.getAvatar());
        vo.setUserLevel(c.getUserLevel());
        vo.setMaskedPhone(c.getMaskedPhone());
        vo.setRegisterTime(c.getRegisterTime());
        vo.setLastSyncAt(c.getLastSyncAt());
        return vo;
    }
}
