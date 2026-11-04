package io.github.foundry4j.quarkforge.notification.service;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.notification.entity.Notification;
import io.github.foundry4j.quarkforge.notification.entity.NotificationRepository;
import io.github.foundry4j.quarkforge.notification.entity.NotificationType;
import io.github.foundry4j.quarkforge.notification.websocket.NotificationWebSocket.SessionManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/// ## NotificationService
/// 通知服务核心实现：
///
/// - 持久化通知 + 尝试实时推送（WebSocket 在线 → 立即送达并标记 `deliveredAt`；
///   离线 → 仅持久化，等用户重连时由 [SessionManager] 补偿）
/// - 查询 / 已读 / 删除全部走仓储
///
/// 实现了 [NotificationSender] SPI，业务模块可直接 `@Inject` 本接口发送通知。
@ApplicationScoped
public class NotificationService implements NotificationSender {

    @Inject
    NotificationRepository repository;

    @Inject
    SessionManager sessionManager;

    // ---------- SPI 实现 ----------

    @Override
    @Transactional
    public void send(String recipientId, NotificationType type, String title, String content,
                     String actorId, String link) {
        if (recipientId == null || recipientId.isBlank()) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "recipientId is required");
        }
        if (title == null || title.isBlank()) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "title is required");
        }

        Notification notification = new Notification();
        notification.recipientId = recipientId;
        notification.type = type;
        notification.title = title;
        notification.content = content;
        notification.actorId = actorId;
        notification.link = link;
        repository.persist(notification);

        // 同步推送：在线 → 送达并标记 delivered；离线 → 等重连补偿
        if (sessionManager.pushTo(recipientId, serialize(notification))) {
            repository.markDelivered(notification.id);
        }
    }

    // ---------- 查询 & 变更 ----------

    /// 当前用户的通知列表。
    public PageResult<Notification> listFor(String recipientId, boolean onlyReadable, int page, int size) {
        return repository.listFor(recipientId, onlyReadable, page, size);
    }

    /// 当前用户未读数。
    public long unreadCount(String recipientId) {
        return repository.unreadCount(recipientId);
    }

    /// 单条已读。
    @Transactional
    public void markRead(String recipientId, long id) {
        Notification n = repository.requireById(id);
        if (!n.recipientId.equals(recipientId)) {
            throw new BizException(CommonErrorCode.FORBIDDEN, "not your notification");
        }
        repository.markRead(id);
    }

    /// 全部已读。
    @Transactional
    public int markAllRead(String recipientId) {
        return repository.markAllRead(recipientId);
    }

    /// 删除通知。
    @Transactional
    public void delete(String recipientId, long id) {
        Notification n = repository.requireById(id);
        if (!n.recipientId.equals(recipientId)) {
            throw new BizException(CommonErrorCode.FORBIDDEN, "not your notification");
        }
        repository.delete(n);
    }

    // ---------- 私有 ----------

    private static String serialize(Notification n) {
        StringBuilder sb = new StringBuilder(192);
        sb.append("{");
        sb.append("\"id\":").append(n.id).append(',');
        sb.append("\"type\":\"").append(n.type).append("\",");
        sb.append("\"title\":\"").append(escape(n.title)).append("\"");
        if (n.content != null) sb.append(",\"content\":\"").append(escape(n.content)).append("\"");
        if (n.link != null) sb.append(",\"link\":\"").append(escape(n.link)).append("\"");
        if (n.actorId != null) sb.append(",\"actorId\":\"").append(escape(n.actorId)).append("\"");
        sb.append(",\"createdAt\":\"").append(n.createdAt).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
