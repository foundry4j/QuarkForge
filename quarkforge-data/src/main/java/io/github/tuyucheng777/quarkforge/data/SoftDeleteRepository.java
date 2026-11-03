package io.github.tuyucheng777.quarkforge.data;

import java.time.Instant;

import jakarta.transaction.Transactional;

import io.github.tuyucheng777.quarkforge.core.api.PageResult;
import io.github.tuyucheng777.quarkforge.core.context.ActorContext;
import io.quarkus.hibernate.orm.panache.PanacheQuery;

/// ## SoftDeleteRepository
/// 软删除仓储基类，默认过滤已删除数据，并提供回收站与恢复能力。
///
/// ```java
/// @ApplicationScoped
/// public class OrderRepository extends SoftDeleteRepository<Order> { }
/// ```
///
/// - [findAll]、[findById]、[count]、[deleteById]、[deleteAll] 都只作用于未删除数据
/// - [findAllIncludingDeleted] / [restoreById] 用于回收站
/// - 分页查询使用继承的 `page()` / `pageAll()`，同样默认过滤
///
/// 注意：直接调用 Panache 的 `find("...")` 或原生 SQL 不会自动带上过滤条件，
/// 需要自行追加 `deleted = false`。
///
/// @param <T> 软删除实体类型
public abstract class SoftDeleteRepository<T extends SoftDeleteEntity> extends BaseRepository<T> {

    private static final String ACTIVE_CONDITION = "deleted = false";

    @Override
    public PanacheQuery<T> findAll() {
        return find(ACTIVE_CONDITION);
    }

    @Override
    public T findById(Long id) {
        return find("id = ?1 and " + ACTIVE_CONDITION, id).firstResultOptional().orElse(null);
    }

    @Override
    public long count() {
        return count(ACTIVE_CONDITION);
    }

    /// 回收站：包含已删除数据。
    public PanacheQuery<T> findAllIncludingDeleted() {
        return find("id is not null");
    }

    /// 回收站：按 ID 查询，包含已删除数据。
    public T findByIdIncludingDeleted(Long id) {
        return find("id = ?1", id).firstResultOptional().orElse(null);
    }

    /// 回收站分页：包含已删除数据。
    public PageResult<T> pageAllIncludingDeleted(int page, int size) {
        return page(find("id is not null"), page, size);
    }

    /// 软删除指定 ID，返回是否命中未删除记录。
    @Transactional
    public boolean deleteById(Long id) {
        return update("deleted = true, deletedAt = ?1, deletedBy = ?2 where id = ?3 and " + ACTIVE_CONDITION,
                Instant.now(), ActorContext.currentOr("system"), id) > 0;
    }

    /// 软删除实体，操作人取 [ActorContext]。
    @Override
    @Transactional
    public void delete(T entity) {
        softDelete(entity);
    }

    /// 软删除实体并指定操作人。
    @Transactional
    public void softDelete(T entity) {
        if (entity == null) {
            return;
        }
        entity.softDelete();
        if (!isPersistent(entity)) {
            getEntityManager().merge(entity);
        }
    }

    /// 软删除全部未删除数据。
    @Override
    @Transactional
    public long deleteAll() {
        return update("deleted = true, deletedAt = ?1, deletedBy = ?2 where " + ACTIVE_CONDITION,
                Instant.now(), ActorContext.currentOr("system"));
    }

    /// 从回收站恢复，返回是否命中已删除记录。
    @Transactional
    public boolean restoreById(Long id) {
        return update("deleted = false, deletedAt = null, deletedBy = null where id = ?1 and deleted = true", id) > 0;
    }

    /// 物理删除指定 ID（谨慎使用）。
    @Transactional
    public boolean purgeById(Long id) {
        return delete("id = ?1", id) > 0;
    }

    /// 物理删除全部数据（谨慎使用）。
    @Transactional
    public long purgeAll() {
        return delete("id is not null");
    }
}
