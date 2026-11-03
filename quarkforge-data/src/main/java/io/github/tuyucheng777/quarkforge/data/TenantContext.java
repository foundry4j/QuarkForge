package io.github.tuyucheng777.quarkforge.data;

import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.ConfigProvider;

/// ## TenantContext
/// 当前租户上下文。设置后，[TenantEntity] 的 `@TenantId` 字段由 Hibernate 自动填充，
/// 并且 Hibernate 会自动在查询上追加 `tenant_id = :tenantId` 条件，业务代码无需手写租户过滤。
///
/// ```java
/// TenantContext.runAs("acme", () -> userRepository.listAll());   // 只会看到 acme 的数据
/// ```
///
/// 未设置时使用 `quarkforge.data.tenant.default`（默认 `default`），
/// 适用于定时任务、后台线程等没有请求上下文的场景。
public final class TenantContext {

    /// 默认租户 ID 的配置键。
    public static final String DEFAULT_TENANT_PROPERTY = "quarkforge.data.tenant.default";

    private static final String FALLBACK_TENANT = "default";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private static volatile String defaultTenant;

    private TenantContext() {
    }

    /// 设置当前租户，`null` 或空白等价于清除。
    public static void set(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            CURRENT.remove();
        } else {
            CURRENT.set(tenantId);
        }
    }

    /// 当前租户；未设置时返回默认租户，保证永不为 `null`。
    public static String current() {
        String tenant = CURRENT.get();
        return tenant == null || tenant.isBlank() ? defaultTenantId() : tenant;
    }

    /// 当前租户；未显式设置时为空。
    public static Optional<String> currentExplicit() {
        return Optional.ofNullable(CURRENT.get());
    }

    /// 在指定租户下执行有返回值的逻辑，结束后恢复原值（支持嵌套）。
    public static <T> T callAs(String tenantId, Supplier<T> action) {
        String previous = CURRENT.get();
        set(tenantId);
        try {
            return action.get();
        } finally {
            restore(previous);
        }
    }

    /// 在指定租户下执行逻辑，结束后恢复原值（支持嵌套）。
    public static void runAs(String tenantId, Runnable action) {
        callAs(tenantId, () -> {
            action.run();
            return null;
        });
    }

    /// 清除当前线程的租户。
    public static void clear() {
        CURRENT.remove();
    }

    /// 默认租户 ID。
    public static String defaultTenantId() {
        String tenant = defaultTenant;
        if (tenant == null) {
            tenant = ConfigProvider.getConfig()
                    .getOptionalValue(DEFAULT_TENANT_PROPERTY, String.class)
                    .filter(value -> !value.isBlank())
                    .orElse(FALLBACK_TENANT);
            defaultTenant = tenant;
        }
        return tenant;
    }

    private static void restore(String previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
