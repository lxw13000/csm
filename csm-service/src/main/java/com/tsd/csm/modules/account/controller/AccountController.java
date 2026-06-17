package com.tsd.csm.modules.account.controller;

import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.RequiresPermission;
import com.tsd.csm.modules.account.domain.dto.AccountQuery;
import com.tsd.csm.modules.account.domain.dto.AccountSaveDTO;
import com.tsd.csm.modules.account.domain.vo.AccountVO;
import com.tsd.csm.modules.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 内部账号管理（本租户）：PC 后台账号与客服端 H5 账号。
 */
@RestController
@RequestMapping("/api/admin/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/page")
    @RequiresPermission("account:list")
    public R<PageResult<AccountVO>> page(AccountQuery query) {
        return R.ok(accountService.pageAccounts(query));
    }

    @GetMapping("/{id}")
    @RequiresPermission("account:list")
    public R<AccountVO> detail(@PathVariable Long id) {
        return R.ok(accountService.toVO(accountService.getById(id)));
    }

    @GetMapping("/agents")
    @RequiresPermission("account:list")
    public R<List<AccountVO>> agents() {
        return R.ok(accountService.listAgents());
    }

    @PostMapping
    @RequiresPermission("account:list")
    public R<AccountVO> create(@RequestBody @Valid AccountSaveDTO dto) {
        return R.ok(accountService.createAccount(dto));
    }

    @PutMapping("/{id}")
    @RequiresPermission("account:list")
    public R<AccountVO> update(@PathVariable Long id, @RequestBody @Valid AccountSaveDTO dto) {
        return R.ok(accountService.updateAccount(id, dto));
    }

    @PutMapping("/{id}/password")
    @RequiresPermission("account:list")
    public R<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        accountService.resetPassword(id, password);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @RequiresPermission("account:list")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        accountService.changeStatus(id, status);
        return R.ok();
    }
}
