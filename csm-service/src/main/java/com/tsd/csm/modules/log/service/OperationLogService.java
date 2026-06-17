package com.tsd.csm.modules.log.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsd.csm.core.common.result.PageResult;
import com.tsd.csm.core.security.LoginUser;
import com.tsd.csm.core.security.UserContext;
import com.tsd.csm.modules.log.domain.OperationLog;
import com.tsd.csm.modules.log.domain.dto.OperationLogQuery;
import com.tsd.csm.modules.log.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作审计日志服务。记录按 {@code TenantContext.appId} 隔离（平台级操作为 _platform_）。
 */
@Service
public class OperationLogService extends ServiceImpl<OperationLogMapper, OperationLog> {

    /** 记录一条操作日志，操作人/IP/UA 从当前请求上下文提取。 */
    public void record(String module, String action, String targetType, String targetId, String detail) {
        OperationLog entry = new OperationLog();
        LoginUser user = UserContext.get();
        entry.setOperatorId(user == null || user.getAccountId() == null ? 0L : user.getAccountId());
        entry.setOperatorName(user == null ? null : user.getRealName());
        entry.setOperatorType(user == null || user.getAccountType() == null ? 0 : user.getAccountType());
        entry.setModule(module);
        entry.setAction(action);
        entry.setTargetType(StringUtils.hasText(targetType) ? targetType : null);
        entry.setTargetId(targetId);
        entry.setDetail(detail);
        HttpServletRequest request = currentRequest();
        if (request != null) {
            entry.setClientIp(clientIp(request));
            entry.setUserAgent(request.getHeader("User-Agent"));
        }
        save(entry);
    }

    /**
     * 分页查询操作日志。
     * @param query 查询条件
     * @return 日志分页结果
     */
    public PageResult<OperationLog> page(OperationLogQuery query) {
        Page<OperationLog> page = lambdaQuery()
                .eq(StringUtils.hasText(query.getModule()), OperationLog::getModule, query.getModule())
                .eq(StringUtils.hasText(query.getAction()), OperationLog::getAction, query.getAction())
                .eq(query.getOperatorId() != null, OperationLog::getOperatorId, query.getOperatorId())
                .orderByDesc(OperationLog::getId)
                .page(Page.of(query.getCurrent(), query.getSize()));
        return PageResult.of(page);
    }

    /** 取当前 HTTP 请求（非 Web 上下文返回 null）。 */
    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /** 解析客户端 IP（优先取 X-Forwarded-For 首段）。 */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
