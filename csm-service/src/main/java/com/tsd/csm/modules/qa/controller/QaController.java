package com.tsd.csm.modules.qa.controller;

import com.tsd.csm.core.audit.AuditLog;
import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.security.RequiresPermission;
import com.tsd.csm.modules.qa.domain.dto.QaQuery;
import com.tsd.csm.modules.qa.domain.dto.QaSaveDTO;
import com.tsd.csm.modules.qa.domain.vo.QaVO;
import com.tsd.csm.modules.qa.service.QaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * QA 知识库管理（本租户）。
 */
@RestController
@RequestMapping("/api/admin/qa")
public class QaController {

    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    /**
     * 分页查询本租户 QA。
     * @param query 查询条件
     * @return QA 分页结果
     */
    @GetMapping("/page")
    @RequiresPermission("qa:list")
    public R<PageResult<QaVO>> page(QaQuery query) {
        return R.ok(qaService.page(query));
    }

    /**
     * QA 详情（含关联关键词）。
     * @param id QA id
     * @return QA 详情
     */
    @GetMapping("/{id}")
    @RequiresPermission("qa:list")
    public R<QaVO> detail(@PathVariable Long id) {
        return R.ok(qaService.detail(id));
    }

    /**
     * 新增 QA。
     * @param dto QA 信息（含关键词）
     * @return 新增的 QA
     */
    @PostMapping
    @RequiresPermission("qa:list")
    @AuditLog(module = "qa", action = "create", targetType = "qa")
    public R<QaVO> create(@RequestBody @Valid QaSaveDTO dto) {
        return R.ok(qaService.create(dto));
    }

    /**
     * 编辑 QA。
     * @param id QA id
     * @param dto QA 信息
     * @return 更新后的 QA
     */
    @PutMapping("/{id}")
    @RequiresPermission("qa:list")
    public R<QaVO> update(@PathVariable Long id, @RequestBody @Valid QaSaveDTO dto) {
        return R.ok(qaService.update(id, dto));
    }

    /**
     * 启用/停用 QA。
     * @param id QA id
     * @param status 状态：1 启用 / 0 停用
     */
    @PutMapping("/{id}/status")
    @RequiresPermission("qa:list")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        qaService.changeStatus(id, status);
        return R.ok();
    }

    /**
     * 删除 QA（连带清除其关键词）。
     * @param id QA id
     */
    @DeleteMapping("/{id}")
    @RequiresPermission("qa:list")
    @AuditLog(module = "qa", action = "delete", targetType = "qa")
    public R<Void> delete(@PathVariable Long id) {
        qaService.delete(id);
        return R.ok();
    }
}
