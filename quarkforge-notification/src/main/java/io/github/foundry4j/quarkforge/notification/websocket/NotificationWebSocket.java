package io.github.foundry4j.quarkforge.notification.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.github.foundry4j.quarkforge.notification.entity.Notification;
import io.github.foundry4j.quarkforge.notification.entity.NotificationRepository;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;

/// ## NotificationWebSocket
/// 实时通知推送端点。前端连接 `/ws/notifications`（或 `/ws/notifications?userId=<id>`）后，
/// 服务端在新通知到达时主动推送 JSON 结构。
///
/// ```json
/// {
///   "id": 1234567890,
///   "type": "TASK",
///   "title": "你有一个新任务",
///   "content": "请尽快处理",
///   "link": "/tasks/123",
///   "actorId": "u001",
///   "createdAt": "2026-11-04T10:00:00Z"
/// }
/// ```
///
/// 鉴权：前端可通过 `?userId=<id>` query 参数或 `X-User-Id` header 传递用户标识；
/// 生产环境建议配合 Quarkus Security JWT，在握手阶段完成身份校验。
@WebSocket(path = "/ws/notifications")
public class NotificationWebSocket {

    @Inject
    SessionManager sessionManager;

    @OnOpen
    void onOpen(WebSocketConnection connection) {
        String userId = resolveUserId(connection);
        if (userId != null) {
            sessionManager.register(userId, connection);
        }
    }

    @OnClose
    void onClose(WebSocketConnection connection) {
        sessionManager.unregister(connection);
    }

    /// 前端心跳：接收 "ping" 并返回 "pong" 以保持连接活跃。
    @OnTextMessage
    public String onMessage(String message) {
        if ("ping".equalsIgnoreCase(message)) {
            return "pong";
        }
        return null;
    }

    /// 从 query param `userId` 或 header `X-User-Id` 推断用户 ID。
    private static String resolveUserId(WebSocketConnection connection) {
        // query 参数优先：/ws/notifications?userId=alice
        String query = connection.handshakeRequest().query();
        if (query != null && !query.isBlank()) {
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && "userId".equals(pair.substring(0, eq))) {
                    return pair.substring(eq + 1);
                }
            }
        }
        // 再查 header
        String header = connection.handshakeRequest().header("X-User-Id");
        if (header != null && !header.isBlank()) {
            return header;
        }
        return null;
    }

    /// ## SessionManager
    /// 会话注册表与推送桥梁 Bean（public 以便 [io.github.foundry4j.quarkforge.notification.service.NotificationService] 注入）。
    @jakarta.enterprise.context.ApplicationScoped
    public static class SessionManager {

        private static final ConcurrentMap<String, WebSocketConnection> SESSIONS = new ConcurrentHashMap<>();

        @Inject
        NotificationRepository repository;

        /// 用户上线：注册会话 + 补发未推送通知。
        public void register(String userId, WebSocketConnection connection) {
            // 先清掉可能残留的旧连接（同一用户多处登录只保留最新一个）
            WebSocketConnection previous = SESSIONS.remove(userId);
            if (previous != null) {
                previous.closeAndAwait();
            }
            SESSIONS.put(userId, connection);
            deliverUndelivered(userId);
        }

        /// 连接关闭：移除映射。
        public void unregister(WebSocketConnection connection) {
            SESSIONS.entrySet().removeIf(e -> e.getValue().id().equals(connection.id()));
        }

        /// 向指定用户推送 JSON 通知。
        ///
        /// @return true 推送成功；false 用户离线或连接已关闭
        public boolean pushTo(String userId, String json) {
            WebSocketConnection connection = SESSIONS.get(userId);
            if (connection == null || connection.isClosed()) {
                return false;
            }
            try {
                connection.sendText(json).await().indefinitely();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        /// 当前在线会话数。
        public int onlineCount() {
            return SESSIONS.size();
        }

        /// 补发给定用户所有未推送通知（`deliveredAt is null`）。
        private void deliverUndelivered(String userId) {
            java.util.List<Notification> undelivered = repository.findUndelivered(userId);
            for (Notification n : undelivered) {
                if (pushTo(userId, serialize(n))) {
                    repository.markDelivered(n.id);
                }
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
}
