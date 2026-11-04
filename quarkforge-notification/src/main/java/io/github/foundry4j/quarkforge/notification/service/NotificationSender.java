package io.github.foundry4j.quarkforge.notification.service;

import io.github.foundry4j.quarkforge.notification.entity.NotificationType;

/// ## NotificationSender
/// 通知发送 SPI：业务模块只依赖此接口，不依赖具体实现，
/// 避免循环依赖并便于测试时 mock。
///
/// 典型用法（业务服务中）：
///
/// ```java
/// @Inject NotificationSender notifier;
///
/// // 任务指派时通知被指派人
/// notifier.send(assigneeId, NotificationType.TASK,
///         "你有一个新任务", "请尽快处理", currentUserId, "/tasks/" + taskId);
/// ```
///
/// 实现默认由 [NotificationService] 提供，会同时完成持久化和 WebSocket 推送尝试。
public interface NotificationSender {

    /// 发送通知。
    ///
    /// @param recipientId 接收人用户 ID（来自 `LoginUser.id()`）
    /// @param type        通知类型
    /// @param title       标题（必填，≤256 字符）
    /// @param content     内容（可选，文本 / Markdown 均可）
    /// @param actorId     触发人用户 ID（系统通知传 `null`）
    /// @param link        关联业务对象 ID 或 URL（可选，前端跳转用）
    void send(String recipientId, NotificationType type, String title, String content,
              String actorId, String link);

    /// 便捷方法：不带关联业务对象。
    default void send(String recipientId, NotificationType type, String title, String content, String actorId) {
        send(recipientId, type, title, content, actorId, null);
    }

    /// 系统通知便捷方法（`actorId` 为 `null`）。
    default void sendSystem(String recipientId, String title, String content) {
        send(recipientId, NotificationType.SYSTEM, title, content, null, null);
    }
}
