package io.github.foundry4j.quarkforge.notification.service;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.notification.channel.NotificationChannel;
import io.github.foundry4j.quarkforge.notification.entity.Notification;
import io.github.foundry4j.quarkforge.notification.entity.NotificationRepository;
import io.github.foundry4j.quarkforge.notification.entity.NotificationType;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/// ## NotificationService
/// 通知服务核心实现：
///
/// - 持久化通知后，依次分发给容器中所有 [NotificationChannel] 通道
///   （WebSocket 站内推送始终启用，邮件 / 短信通道按配置启用，业务方可自定义通道）；
///   单个通道失败只记警告，不影响其他通道与主流程
/// - 查询 / 已读 / 删除全部走仓储
///
/// 实现了 [NotificationSender] SPI，业务模块可直接 `@Inject` 本接口发送通知。
@ApplicationScoped
public class NotificationService implements NotificationSender {

    @Inject
    NotificationRepository repository;

    // Instance 注入点让 Arc 保留所有通道实现（否则反射式遍历到的 Bean 会被构建期移除）
    @Inject
    Instance<NotificationChannel> channels;

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

        for (NotificationChannel channel : channels) {
            try {
                channel.deliver(notification);
            } catch (Exception e) {
                Log.warnf(e, "通知通道 %s 投递失败（通知 %d）", channel.name(), notification.id);
            }
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
}
