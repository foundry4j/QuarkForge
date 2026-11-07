package io.github.foundry4j.quarkforge.audit;

import io.github.foundry4j.quarkforge.data.BaseEntity;
import io.github.foundry4j.quarkforge.data.NotAudited;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/// ## OperationLogEntry
/// 操作日志，每条记录描述一次被 [OperationLog] 标注的方法调用：
///
/// - `module` / `action` / `description`：业务模块、操作类型与描述
/// - `actor`：操作人（JWT 主体、`ActorContext` 或 `system`）
/// - `method`：被调用的方法（`类名#方法名`）
/// - `args`：方法参数快照（JSON）
/// - `success` / `errorMessage`：执行结果
/// - `requestMethod` / `requestPath` / `ip` / `userAgent`：web 请求信息（非 web 场景为空）
/// - `traceId`：链路 ID
/// - `durationMs`：执行耗时（毫秒）
/// - `occurredAt`：开始执行时间
///
/// 标记 [NotAudited]，避免操作日志自身被实体审计再次记录。
@Entity
@Table(name = "qf_operation_log",
        indexes = {
                @Index(name = "idx_qf_oplog_actor", columnList = "actor, occurred_at"),
                @Index(name = "idx_qf_oplog_module", columnList = "module_name, occurred_at"),
                @Index(name = "idx_qf_oplog_occurred", columnList = "occurred_at")
        })
@NotAudited
public class OperationLogEntry extends BaseEntity {

    /// 业务模块。
    @Column(name = "module_name", length = 128)
    public String module;

    /// 操作类型。
    @Column(length = 64)
    public String action;

    /// 操作描述。
    @Column(length = 512)
    public String description;

    /// 操作人。
    @Column(nullable = false, length = 64)
    public String actor;

    /// 被调用的方法（`类名#方法名`）。
    @Column(length = 256)
    public String method;

    /// 方法参数快照（JSON）。
    @Column(columnDefinition = "text")
    public String args;

    /// 是否执行成功。
    @Column(nullable = false)
    public boolean success;

    /// 失败时的错误信息。
    @Column(name = "error_message", columnDefinition = "text")
    public String errorMessage;

    /// HTTP 请求方法。
    @Column(name = "request_method", length = 16)
    public String requestMethod;

    /// HTTP 请求路径。
    @Column(name = "request_path", length = 512)
    public String requestPath;

    /// 客户端 IP（取自 `X-Forwarded-For` / `X-Real-IP`）。
    @Column(length = 64)
    public String ip;

    /// 客户端 User-Agent。
    @Column(name = "user_agent", length = 512)
    public String userAgent;

    /// 链路 ID。
    @Column(name = "trace_id", length = 64)
    public String traceId;

    /// 执行耗时（毫秒）。
    @Column(name = "duration_ms", nullable = false)
    public long durationMs;

    /// 开始执行时间。
    @Column(name = "occurred_at", nullable = false)
    public Instant occurredAt;
}
