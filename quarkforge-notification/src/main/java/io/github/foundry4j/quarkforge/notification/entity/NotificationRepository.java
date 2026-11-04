package io.github.foundry4j.quarkforge.notification.entity;

import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.data.BaseRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

/// ## NotificationRepository
/// 通知仓储：查询按「接收人 + 时间」降序返回，未读数与推送补偿查询走
/// 专门的 JPQL 方法（不依赖 Panache 自动生成的查询）。
///
/// 注意：更新操作全部用对象级修改（find → set → persist），
/// 保持 Hibernate 一级缓存与数据库同步，避免 bulk update 后的缓存不一致。
@ApplicationScoped
public class NotificationRepository extends BaseRepository<Notification> {

    /// 当前用户的通知列表，按创建时间降序。
    public PageResult<Notification> listFor(String recipientId, boolean onlyReadable, int page, int size) {
        String query = onlyReadable
                ? "recipientId = ?1 and readAt is null order by createdAt desc"
                : "recipientId = ?1 order by createdAt desc";
        return page(find(query, recipientId), page, size);
    }

    /// 当前用户未读通知数。
    public long unreadCount(String recipientId) {
        return count("recipientId = ?1 and readAt is null", recipientId);
    }

    /// 当前用户未推送通知数（WebSocket 断线重连补偿用）。
    public long undeliveredCount(String recipientId) {
        return count("recipientId = ?1 and deliveredAt is null", recipientId);
    }

    /// 当前用户所有未推送通知。
    public java.util.List<Notification> findUndelivered(String recipientId) {
        return list("recipientId = ?1 and deliveredAt is null order by createdAt asc", recipientId);
    }

    /// 单条已读（对象级操作，自动 flush）。
    public void markRead(long id) {
        Notification n = findById(id);
        if (n != null && n.readAt == null) {
            n.readAt = Instant.now();
        }
    }

    /// 批量已读，返回更新的条数。
    public int markAllRead(String recipientId) {
        java.util.List<Notification> unread = list("recipientId = ?1 and readAt is null", recipientId);
        Instant now = Instant.now();
        for (Notification n : unread) {
            n.readAt = now;
        }
        return unread.size();
    }

    /// 标记为已推送（对象级操作）。
    public void markDelivered(long id) {
        Notification n = findById(id);
        if (n != null && n.deliveredAt == null) {
            n.deliveredAt = Instant.now();
        }
    }
}
