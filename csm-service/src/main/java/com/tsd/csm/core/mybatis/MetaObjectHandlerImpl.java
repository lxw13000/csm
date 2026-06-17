package com.tsd.csm.core.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充 created_at / updated_at。
 *
 * <p>app_id 的注入由 {@code TenantLineInnerInterceptor} 在 SQL 层完成
 * （见 {@link com.tsd.csm.core.config.MybatisPlusConfig}），此处不重复处理。
 */
@Component
public class MetaObjectHandlerImpl implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        if (metaObject.hasGetter("createdAt") && metaObject.getValue("createdAt") == null) {
            this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        }
        if (metaObject.hasGetter("updatedAt") && metaObject.getValue("updatedAt") == null) {
            this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasGetter("updatedAt")) {
            this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        }
    }
}
