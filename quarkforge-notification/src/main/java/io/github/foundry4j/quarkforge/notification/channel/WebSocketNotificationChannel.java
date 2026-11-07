package io.github.foundry4j.quarkforge.notification.channel;

import io.github.foundry4j.quarkforge.notification.entity.Notification;
import io.github.foundry4j.quarkforge.notification.entity.NotificationRepository;
import io.github.foundry4j.quarkforge.notification.websocket.NotificationWebSocket.SessionManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/// ## WebSocketNotificationChannel
/// 内置站内实时推送通道（始终启用）：用户在线 → 立即送达并标记 `deliveredAt`；
/// 离线 → 仅持久化，等重连时由 [SessionManager] 补偿推送。
@ApplicationScoped
public class WebSocketNotificationChannel implements NotificationChannel {

    @Inject
    SessionManager sessionManager;

    @Inject
    NotificationRepository repository;

    @Override
    public String name() {
        return "websocket";
    }

    @Override
    public void deliver(Notification notification) {
        if (sessionManager.pushTo(notification.recipientId, serialize(notification))) {
            repository.markDelivered(notification.id);
        }
    }

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
