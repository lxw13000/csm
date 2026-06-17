package com.tsd.csm.modules.customer.controller;

import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.RequiresPermission;
import com.tsd.csm.modules.customer.domain.vo.CustomerVO;
import com.tsd.csm.modules.customer.service.CustomerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端用户查看（只读）。PC 管理端按 app_id + user_id 检索本租户用户；详情实时查询业务系统。
 */
@RestController
@RequestMapping("/api/admin/customer")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * 分页检索本租户 C 端用户缓存。
     * @param current 页码
     * @param size 每页条数
     * @param keyword user_id/昵称模糊词，可空
     * @return 用户分页结果
     */
    @GetMapping("/page")
    @RequiresPermission("customer:list")
    public R<PageResult<CustomerVO>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword) {
        return R.ok(customerService.page(current, size, keyword));
    }

    /**
     * 实时查询用户详情（优先业务系统，失败降级缓存）。
     * @param userId 业务系统用户 id
     * @return 用户详情
     */
    @GetMapping("/detail")
    @RequiresPermission("customer:list")
    public R<CustomerVO> detail(@RequestParam String userId) {
        return R.ok(customerService.detailRealtime(userId));
    }
}
