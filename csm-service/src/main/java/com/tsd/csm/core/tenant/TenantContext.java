package com.tsd.csm.core.tenant;

/**
 * 租户上下文（线程级）。承载当前请求的隔离键 app_id，
 * 供 {@link CsmTenantLineHandler} 在 SQL 层自动追加 {@code app_id = ?}。
 *
 * <p>{@code ignore=true} 时跳过租户过滤，用于平台超管的跨租户只读统计路径。
 */
public final class TenantContext {

    private static final ThreadLocal<String> APP_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TenantContext() {
    }

    public static void setAppId(String appId) {
        APP_ID.set(appId);
    }

    public static String getAppId() {
        return APP_ID.get();
    }

    public static void setIgnore(boolean ignore) {
        IGNORE.set(ignore);
    }

    public static boolean isIgnore() {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    /** 在忽略租户过滤的上下文中执行（如跨租户统计），结束后自动恢复。 */
    public static <T> T executeIgnore(java.util.function.Supplier<T> supplier) {
        boolean old = isIgnore();
        setIgnore(true);
        try {
            return supplier.get();
        } finally {
            setIgnore(old);
        }
    }

    /** 在忽略租户过滤的上下文中执行（无返回值），结束后自动恢复。 */
    public static void runIgnore(Runnable runnable) {
        boolean old = isIgnore();
        setIgnore(true);
        try {
            runnable.run();
        } finally {
            setIgnore(old);
        }
    }

    /** 在指定租户上下文中执行并返回结果（如登录时按 app_id 查账号），结束后自动恢复。 */
    public static <T> T callWithAppId(String appId, java.util.function.Supplier<T> supplier) {
        String old = getAppId();
        setAppId(appId);
        try {
            return supplier.get();
        } finally {
            setAppId(old);
        }
    }

    /** 在指定租户上下文中执行（如平台超管为某新租户预置配置），结束后自动恢复。 */
    public static void runWithAppId(String appId, Runnable runnable) {
        String old = getAppId();
        setAppId(appId);
        try {
            runnable.run();
        } finally {
            setAppId(old);
        }
    }

    public static void clear() {
        APP_ID.remove();
        IGNORE.remove();
    }
}
