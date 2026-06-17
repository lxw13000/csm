package com.tsd.csm.modules.customer.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.core.tenant.TenantContext;
import com.tsd.csm.core.util.MaskUtil;
import com.tsd.csm.modules.customer.domain.Customer;
import com.tsd.csm.modules.customer.domain.vo.CustomerVO;
import com.tsd.csm.modules.customer.mapper.CustomerMapper;
import com.tsd.csm.modules.integration.client.BizSystemClient;
import com.tsd.csm.modules.integration.domain.CustomerInfo;
import com.tsd.csm.modules.tenant.domain.Tenant;
import com.tsd.csm.modules.tenant.service.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * C 端用户缓存服务：首次接入缓存 + 按需实时查询（呼应 xuqiu.md 2.5）。
 */
@Service
public class CustomerService extends ServiceImpl<CustomerMapper, Customer> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BizSystemClient bizClient;
    private final TenantService tenantService;

    public CustomerService(BizSystemClient bizClient, TenantService tenantService) {
        this.bizClient = bizClient;
        this.tenantService = tenantService;
    }

    /** 取用户缓存记录（无则返回 null）。 */
    public Customer getCached(String userId) {
        return lambdaQuery().eq(Customer::getUserId, userId).one();
    }

    /** 首次接入时缓存基础信息；已存在则返回缓存。要求 TenantContext.appId 已设置为该租户。 */
    public Customer ensureCached(Tenant tenant, String userId) {
        Customer existing = getCached(userId);
        if (existing != null) {
            return existing;
        }
        CustomerInfo info = bizClient.queryUserInfo(tenant, userId);
        Customer c = new Customer();
        c.setUserId(userId);
        applyInfo(c, info);
        c.setLastSyncAt(info != null ? LocalDateTime.now() : null);
        save(c);
        return c;
    }

    /** 实时查询用户详情：优先调业务系统刷新缓存；不可用则降级返回缓存并标记非最新。 */
    public CustomerVO detailRealtime(String userId) {
        String appId = TenantContext.getAppId();
        Tenant tenant = tenantService.getByAppId(appId);
        Customer cached = getCached(userId);
        CustomerInfo info = tenant == null ? null : bizClient.queryUserInfo(tenant, userId);

        if (info != null) {
            if (cached == null) {
                cached = new Customer();
                cached.setUserId(userId);
            }
            applyInfo(cached, info);
            cached.setLastSyncAt(LocalDateTime.now());
            saveOrUpdate(cached);
            return toVO(cached, true);
        }
        // 降级：返回缓存
        if (cached == null) {
            CustomerVO vo = new CustomerVO();
            vo.setUserId(userId);
            vo.setLatest(false);
            return vo;
        }
        return toVO(cached, false);
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
        return PageResult.of(page, c -> toVO(c, false));
    }

    /** 将业务系统返回的用户信息写入缓存实体（手机号脱敏）。 */
    private void applyInfo(Customer c, CustomerInfo info) {
        if (info == null) {
            return;
        }
        c.setNickname(info.getNickname());
        c.setAvatar(info.getAvatar());
        c.setUserLevel(info.getUserLevel());
        c.setMaskedPhone(MaskUtil.maskPhone(info.getPhone()));
        c.setRegisterTime(parse(info.getRegisterTime()));
    }

    /**
     * 缓存实体转展示 VO。
     * @param c 用户缓存实体
     * @param latest 是否为业务系统实时数据
     * @return 用户 VO
     */
    public CustomerVO toVO(Customer c, boolean latest) {
        CustomerVO vo = new CustomerVO();
        vo.setUserId(c.getUserId());
        vo.setNickname(c.getNickname());
        vo.setAvatar(c.getAvatar());
        vo.setUserLevel(c.getUserLevel());
        vo.setMaskedPhone(c.getMaskedPhone());
        vo.setRegisterTime(c.getRegisterTime());
        vo.setLastSyncAt(c.getLastSyncAt());
        vo.setLatest(latest);
        return vo;
    }

    /** 解析 yyyy-MM-dd HH:mm:ss 时间字符串，空或非法返回 null。 */
    private LocalDateTime parse(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return LocalDateTime.parse(s, FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
