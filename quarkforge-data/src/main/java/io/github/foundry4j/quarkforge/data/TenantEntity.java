package io.github.foundry4j.quarkforge.data;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/// ## TenantEntity
/// 多租户实体基类。继承后自动获得 `tenant_id` 字段：
///
/// - 写入时从 [TenantContext] 自动填充，无需业务代码赋值
/// - 查询时 Hibernate 自动追加 `tenant_id = :tenantId` 条件（含 `findById`），
///   A 租户天然看不到 B 租户的数据
///
/// ```java
/// @Entity
/// public class Order extends TenantEntity {
///     public String orderNo;
/// }
///
/// TenantContext.runAs("acme", () -> orderRepository.listAll());   // 仅 acme 的订单
/// ```
///
/// 实现方式为 Hibernate 自动启用的过滤器（`autoEnabled = true`），
/// 不依赖请求上下文，因此在定时任务、消息消费等后台线程中同样生效。
/// 未设置租户时使用 `quarkforge.data.tenant.default`（默认 `default`）。
@FilterDef(
        name = "tenantFilter",
        defaultCondition = "tenant_id = :tenantId",
        parameters = @ParamDef(name = "tenantId", type = String.class, resolver = TenantFilterParameterResolver.class),
        autoEnabled = true,
        applyToLoadByKey = true)
@Filter(name = "tenantFilter")
@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {

    /// 租户标识，写入后不可修改。
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    public String tenantId;

    @PrePersist
    void assignTenant() {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = TenantContext.current();
        }
    }
}
