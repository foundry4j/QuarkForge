package io.github.foundry4j.quarkforge.notification;

import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.notification.entity.Notification;
import io.github.foundry4j.quarkforge.notification.entity.NotificationType;
import io.github.foundry4j.quarkforge.notification.service.NotificationService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// NotificationService 的端到端测试：验证持久化、查询、已读、删除全流程。
@QuarkusTest
class NotificationServiceTest {

    @Inject
    NotificationService service;

    @Inject
    io.github.foundry4j.quarkforge.notification.entity.NotificationRepository repository;

    @BeforeEach
    @Transactional
    void tearDown() {
        repository.deleteAll();
    }

    // ---------- 发送 ----------

    @Test
    @Transactional
    void persistsNotification() {
        service.send("alice", NotificationType.TASK, "新任务", "请处理", "bob", "/tasks/42");

        assertEquals(1, repository.unreadCount("alice"));
        PageResult<Notification> page = service.listFor("alice", false, 1, 20);
        assertEquals(1, page.total());
        Notification n = page.records().getFirst();
        assertEquals("alice", n.recipientId);
        assertEquals(NotificationType.TASK, n.type);
        assertEquals("新任务", n.title);
        assertEquals("请处理", n.content);
        assertEquals("bob", n.actorId);
        assertEquals("/tasks/42", n.link);
        assertFalse(n.isRead());
    }

    @Test
    @Transactional
    void sendsSystemNotification() {
        service.sendSystem("alice", "公告", "系统维护通知");

        PageResult<Notification> page = service.listFor("alice", false, 1, 20);
        assertEquals(1, page.total());
        Notification n = page.records().getFirst();
        assertEquals(NotificationType.SYSTEM, n.type);
        // System notification: actorId is null
        assertTrue(n.actorId == null || n.actorId.isBlank());
    }

    @Test
    void rejectsBlankRecipientId() {
        assertThrows(BizException.class,
                () -> service.send("", NotificationType.TASK, "t", "c", "a"));
    }

    @Test
    void rejectsBlankTitle() {
        assertThrows(BizException.class,
                () -> service.send("alice", NotificationType.TASK, "", "c", "a"));
    }

    // ---------- 已读 ----------

    @Test
    @Transactional
    void marksSingleNotificationRead() {
        service.send("alice", NotificationType.TASK, "t", "c", "bob");

        long id = repository.find("recipientId", "alice").singleResult().id;
        service.markRead("alice", id);

        assertEquals(0, repository.unreadCount("alice"));
        assertTrue(repository.findById(id).isRead());
    }

    @Test
    @Transactional
    void marksAllAsRead() {
        for (int i = 0; i < 5; i++) {
            service.send("alice", NotificationType.SYSTEM, "t" + i, "c", null);
        }
        assertEquals(5, repository.unreadCount("alice"));

        int marked = service.markAllRead("alice");
        assertEquals(5, marked);
        assertEquals(0, repository.unreadCount("alice"));
    }

    @Test
    @Transactional
    void cannotMarkOthersNotification() {
        service.send("bob", NotificationType.TASK, "t", "c", null);
        long id = repository.find("recipientId", "bob").singleResult().id;

        assertThrows(BizException.class, () -> service.markRead("alice", id));
        assertThrows(BizException.class, () -> service.delete("alice", id));
    }

    // ---------- 查询 ----------

    @Test
    @Transactional
    void listPaginatesAndFiltersUnread() {
        for (int i = 0; i < 15; i++) {
            service.send("alice", NotificationType.SYSTEM, "t" + i, "c", null);
        }

        PageResult<Notification> page1 = service.listFor("alice", false, 1, 5);
        assertEquals(15, page1.total());
        assertEquals(5, page1.records().size());

        service.markAllRead("alice");
        // 全已读后，未读过滤器返回空
        PageResult<Notification> unread = service.listFor("alice", true, 1, 20);
        assertEquals(0, unread.total());

        // 另一个用户看不到 alice 的通知（多租户 + recipientId 双重过滤）
        PageResult<Notification> bobPage = service.listFor("bob", false, 1, 20);
        assertEquals(0, bobPage.total());
    }

    @Test
    @Transactional
    void deletesNotification() {
        service.send("alice", NotificationType.TASK, "t", "c", null);
        long id = repository.find("recipientId", "alice").singleResult().id;

        service.delete("alice", id);
        assertTrue(repository.findByIdOptional(id).isEmpty());
    }
}
