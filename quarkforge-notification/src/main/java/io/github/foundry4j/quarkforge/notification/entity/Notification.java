package io.github.foundry4j.quarkforge.notification.entity;

import java.time.Instant;

import io.github.foundry4j.quarkforge.data.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/// ## Notification
/// 通知实体，继承 [TenantEntity] 天然获得多租户隔离。
///
/// 核心字段：
///
/// - `recipientId`：接收人用户 ID（来自 `LoginUser.id()`）
/// - `type` / `title` / `content`：通知内容
/// - `link`：关联业务对象 ID 或 URL，前端点击跳转用
/// - `actorId`：触发人（系统通知为 `null`）
/// - `readAt`：已读时间（`null` 表示未读）
/// - `deliveredAt`：推送时间（`null` 表示尚未推送，WebSocket 断线重连时补偿）
///
/// 数据库索引以「接收人 + 时间」为最常用查询路径，保证列表查询与未读数统计高效。
@Entity
@Table(name = "qf_notification", indexes = {
        @Index(name = "idx_notif_recipient_created", columnList = "recipient_id, created_at desc"),
        @Index(name = "idx_notif_recipient_read", columnList = "recipient_id, read_at"),
        @Index(name = "idx_notif_recipient_delivered", columnList = "recipient_id, delivered_at")})
public class Notification extends TenantEntity {

    @Column(name = "recipient_id", nullable = false, length = 64)
    public String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public NotificationType type;

    @Column(nullable = false, length = 256)
    public String title;

    @Column(columnDefinition = "TEXT")
    public String content;

    @Column(length = 512)
    public String link;

    @Column(name = "actor_id", length = 64)
    public String actorId;

    @Column(name = "read_at")
    public Instant readAt;

    @Column(name = "delivered_at")
    public Instant deliveredAt;

    /// 是否已读（`readAt != null`）。
    public boolean isRead() {
        return readAt != null;
    }

    /// 是否已推送（`deliveredAt != null`）。
    public boolean isDelivered() {
        return deliveredAt != null;
    }
}
