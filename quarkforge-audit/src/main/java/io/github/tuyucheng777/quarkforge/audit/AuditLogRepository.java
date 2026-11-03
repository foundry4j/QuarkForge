package io.github.tuyucheng777.quarkforge.audit;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.github.tuyucheng777.quarkforge.core.api.PageResult;
import io.github.tuyucheng777.quarkforge.data.BaseRepository;

/// ## AuditLogRepository
/// 审计日志查询。
///
/// ```java
/// // 查看某条订单的全部变更历史
/// List<AuditLog> history = auditLogRepository.findByEntity("com.acme.Order", orderId);
/// ```
@ApplicationScoped
public class AuditLogRepository extends BaseRepository<AuditLog> {

    /// 按实体类型与主键查询变更历史（按发生时间升序）。
    public List<AuditLog> findByEntity(String entityType, Object entityId) {
        return list("entityType = ?1 and entityId = ?2 order by occurredAt, id",
                entityType, String.valueOf(entityId));
    }

    /// 按实体类型与主键分页查询。
    public PageResult<AuditLog> pageByEntity(String entityType, Object entityId, int page, int size) {
        return page(find("entityType = ?1 and entityId = ?2 order by occurredAt",
                entityType, String.valueOf(entityId)), page, size);
    }

    /// 按操作人分页查询。
    public PageResult<AuditLog> pageByActor(String actor, int page, int size) {
        return page(find("actor = ?1 order by occurredAt", actor), page, size);
    }
}
