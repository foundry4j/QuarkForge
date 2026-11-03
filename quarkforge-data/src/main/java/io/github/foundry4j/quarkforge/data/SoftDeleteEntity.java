package io.github.foundry4j.quarkforge.data;

import io.github.foundry4j.quarkforge.core.context.ActorContext;
import io.quarkus.hibernate.orm.panache.Panache;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.time.Instant;

/// ## SoftDeleteEntity
/// 软删除实体基类。删除只写入标记，数据仍保留：
///
/// - `deleted`：是否已删除，列表默认过滤
/// - `deletedAt` / `deletedBy`：删除时间与操作人
///
/// 配合 [SoftDeleteRepository] 使用，可直接获得「默认只看未删除数据、支持回收站恢复」：
///
/// ```java
/// @Entity
/// public class Order extends SoftDeleteEntity {
///     public String orderNo;
/// }
///
/// @ApplicationScoped
/// public class OrderRepository extends SoftDeleteRepository<Order> { }
///
/// orderRepository.deleteById(id);          // 软删除
/// orderRepository.findAll();               // 自动排除已删除
/// orderRepository.findAllIncludingDeleted(); // 回收站
/// orderRepository.restoreById(id);         // 恢复
/// ```
@MappedSuperclass
public abstract class SoftDeleteEntity extends BaseEntity {

    /// 是否已删除。
    @Column(name = "deleted", nullable = false)
    public boolean deleted;

    /// 删除时间，未删除时为 `null`。
    @Column(name = "deleted_at")
    public Instant deletedAt;

    /// 删除操作人。
    @Column(name = "deleted_by", length = 64)
    public String deletedBy;

    /// 是否已软删除。
    public boolean isDeleted() {
        return deleted;
    }

    /// 软删除，操作人取 [ActorContext]，未设置时为 `system`。
    public void softDelete() {
        softDelete(ActorContext.currentOr("system"));
    }

    /// 软删除并指定操作人。
    public void softDelete(String operator) {
        if (deleted) {
            return;
        }
        deleted = true;
        deletedAt = Instant.now();
        deletedBy = operator == null || operator.isBlank() ? "system" : operator;
    }

    /// 从回收站恢复。
    public void restore() {
        deleted = false;
        deletedAt = null;
        deletedBy = null;
    }

    /// 覆盖 Panache 的实例删除方法，默认走软删除。
    @Override
    public void delete() {
        softDelete();
    }

    /// 物理删除（谨慎使用，仅用于数据清理）。
    public void purge() {
        Panache.getEntityManager().remove(this);
    }
}
