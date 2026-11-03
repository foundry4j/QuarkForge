package io.github.foundry4j.quarkforge.outbox;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import io.github.foundry4j.quarkforge.data.BaseEntity;

/// ## OutboxMessage
/// 本地消息表实体：与业务数据处于同一个数据库事务，保证「业务落库」与「消息发送」的一致性。
///
/// 由 [OutboxService] 写入、[OutboxDispatcher] 投递。字段含义：
///
/// - `destination`：目标地址，RabbitMQ 中作为 routing key
/// - `payload`：JSON 负载
/// - `status` / `attempts`：投递状态与已尝试次数
/// - `nextAttemptAt`：下次可投递时间，延迟消息与失败退避都依赖该字段
/// - `lastError`：最近一次失败原因，便于排查
@Entity
@Table(name = "qf_outbox_message",
        indexes = @Index(name = "idx_qf_outbox_dispatch", columnList = "status, next_attempt_at"))
public class OutboxMessage extends BaseEntity {

    @Column(name = "message_id", length = 64)
    public String messageId;

    @Column(name = "destination", nullable = false, length = 200)
    public String destination;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    public String payload;

    @Column(name = "headers", columnDefinition = "text")
    public String headers;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    public OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    public int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    public Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "text")
    public String lastError;

    @Column(name = "sent_at")
    public Instant sentAt;
}
