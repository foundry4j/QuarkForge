package io.github.tuyucheng777.quarkforge.data;

import java.util.function.Supplier;

/// 为 Hibernate 自动启用的租户过滤器提供 `tenantId` 参数值。
/// 每次渲染查询时读取 [TenantContext]，因此同一 Session 内切换租户也能立即生效。
public class TenantFilterParameterResolver implements Supplier<String> {

    @Override
    public String get() {
        return TenantContext.current();
    }
}
