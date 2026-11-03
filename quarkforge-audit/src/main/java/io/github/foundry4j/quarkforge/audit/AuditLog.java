package io.github.foundry4j.quarkforge.audit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import io.github.foundry4j.quarkforge.data.BaseEntity;
import io.github.foundry4j.quarkforge.data.NotAudited;

/// ## AuditLog
/// 审计日志。每条记录描述某个实体的一次新增、更新或删除：
///
/// - `entityType` / `entityId`：被操作实体
/// - `action`：`CREATE` / `UPDATE` / `DELETE`
/// - `actor`：操作人（JWT 主体、[ActorContext] 或 `system`）
/// - `changes`：JSON，更新时为字段级差异，新增/删除时为字段快照
/// - `occurredAt`：发生时间
///
/// 标记 [NotAudited]，避免审计日志自身被再次审计。
@Entity
@Table(name = "qf_audit_log",
        indexes = {
                @Index(name = "idx_qf_audit_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_qf_audit_occurred", columnList = "occurred_at")
        })
@NotAudited
public class AuditLog extends BaseEntity {

    /// 实体类型（全限定类名）。
    @Column(name = "entity_type", nullable = false, length = 256)
    public String entityType;

    /// 实体主键（字符串形式）。
    @Column(name = "entity_id", length = 64)
    public String entityId;

    /// 操作类型：CREATE / UPDATE / DELETE。
    @Column(nullable = false, length = 16)
    public String action;

    /// 操作人。
    @Column(nullable = false, length = 64)
    public String actor;

    /// 字段级变更或字段快照（JSON）。
    @Column(columnDefinition = "text")
    public String changes;

    /// 发生时间。
    @Column(name = "occurred_at", nullable = false)
    public Instant occurredAt;
}
