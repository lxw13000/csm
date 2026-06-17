package com.tsd.csm.core.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.tsd.csm.core.common.constant.CsmConst;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 租户行级隔离处理器。
 *
 * <ul>
 *   <li>隔离列：{@code app_id}（VARCHAR，以 {@link StringValue} 注入）。</li>
 *   <li>忽略表：全局字典 {@code csm_menu}、租户注册表 {@code csm_tenant}
 *       （其 app_id 为业务主键，由平台超管显式维护，不参与行级过滤）。</li>
 *   <li>当 {@link TenantContext#isIgnore()} 为真时，对所有表跳过过滤。</li>
 * </ul>
 */
public class CsmTenantLineHandler implements TenantLineHandler {

    private static final Set<String> IGNORE_TABLES = Set.of("csm_menu", "csm_tenant");

    @Override
    public Expression getTenantId() {
        String appId = TenantContext.getAppId();
        // 上下文缺失时返回一个不可能匹配的值，宁可查不到也不跨租户泄露
        return new StringValue(StringUtils.hasText(appId) ? appId : "__none__");
    }

    @Override
    public String getTenantIdColumn() {
        return "app_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (TenantContext.isIgnore()) {
            return true;
        }
        return IGNORE_TABLES.contains(stripName(tableName));
    }

    private String stripName(String tableName) {
        if (tableName == null) {
            return "";
        }
        return tableName.replace("`", "").trim().toLowerCase();
    }

    /** 平台保留值常量复用，避免硬编码散落。 */
    public static String platformAppId() {
        return CsmConst.PLATFORM_APP_ID;
    }
}
