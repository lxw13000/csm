package com.tsd.csm.core.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计注解。标注在管理端写操作方法上，由 {@code LogAspect} 切面落库到 csm_operation_log。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 操作模块，如 ticket/qa/account/tenant/config。 */
    String module();

    /** 操作动作，如 create/update/delete/close/transfer。 */
    String action();

    /** 目标对象类型（可选）。 */
    String targetType() default "";
}
