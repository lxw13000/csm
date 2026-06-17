package com.tsd.csm.modules.log.aspect;

import com.tsd.csm.core.audit.AuditLog;
import com.tsd.csm.modules.log.service.OperationLogService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 操作审计切面：方法正常返回后记录 {@link AuditLog} 标注的操作。审计失败不影响主流程。
 */
@Aspect
@Component
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    private final OperationLogService operationLogService;

    public LogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @AfterReturning(pointcut = "@annotation(auditLog)")
    public void afterReturning(AuditLog auditLog) {
        try {
            operationLogService.record(auditLog.module(), auditLog.action(), auditLog.targetType(), null, null);
        } catch (Exception e) {
            log.warn("操作审计记录失败 module={} action={}, err={}",
                    auditLog.module(), auditLog.action(), e.getMessage());
        }
    }
}
