package io.github.foundry4j.quarkforge.notification;

import io.github.foundry4j.quarkforge.notification.channel.NotificationChannel;
import io.github.foundry4j.quarkforge.notification.channel.NotificationContactResolver;
import io.github.foundry4j.quarkforge.notification.entity.Notification;
import io.github.foundry4j.quarkforge.notification.entity.NotificationRepository;
import io.github.foundry4j.quarkforge.notification.entity.NotificationType;
import io.github.foundry4j.quarkforge.notification.service.NotificationService;
import io.github.foundry4j.quarkforge.notification.sms.SmsSender;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// 通知通道测试：验证分发机制（含异常隔离）、邮件通道与短信通道。
/// 配置见 test/resources/application.properties（email/sms 通道已启用，邮件走 MockMailbox）。
@QuarkusTest
class NotificationChannelTest {

    @Inject
    NotificationService service;

    @Inject
    NotificationRepository repository;

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    @Transactional
    void reset() {
        repository.deleteAll();
        mailbox.clear();
        RecordingChannel.DELIVERED.clear();
        RecordingSmsSender.SENT.clear();
    }

    @Test
    @Transactional
    void dispatchesToAllChannelsAndIsolatesFailures() {
        // FailingChannel 总是抛异常，但分发不应中断，RecordingChannel 仍应收到通知
        service.send("bob", NotificationType.TASK, "任务A", "请处理", "alice", null);

        assertEquals(List.of("任务A"), RecordingChannel.DELIVERED);
        assertEquals(1, repository.count());
    }

    @Test
    @Transactional
    void emailChannelSendsToResolvedAddress() {
        service.send("alice", NotificationType.APPROVAL, "审批通过", "你的申请已通过", "system", null);

        List<Mail> mails = mailbox.getMessagesSentTo("alice@example.com");
        assertEquals(1, mails.size());
        assertEquals("审批通过", mails.getFirst().getSubject());
        assertEquals("你的申请已通过", mails.getFirst().getText());
    }

    @Test
    @Transactional
    void smsChannelSendsToResolvedPhone() {
        service.send("alice", NotificationType.SYSTEM, "系统维护", "今晚 24:00 停机升级", null, null);

        assertEquals(1, RecordingSmsSender.SENT.size());
        String sent = RecordingSmsSender.SENT.getFirst();
        assertTrue(sent.startsWith("13800000000|"), "短信应发往解析出的手机号: " + sent);
        assertTrue(sent.contains("系统维护") && sent.contains("今晚 24:00 停机升级"),
                "短信内容应包含标题与正文: " + sent);
    }

    @Test
    @Transactional
    void emailAndSmsSkipWhenContactMissing() {
        // bob 在 TestContactResolver 中没有邮箱 / 手机号映射，两个通道都应静默跳过
        service.send("bob", NotificationType.SYSTEM, "公告", "内容", null, null);

        assertTrue(mailbox.getMessagesSentTo("bob@example.com").isEmpty());
        assertEquals(0, mailbox.getTotalMessagesSent());
        assertTrue(RecordingSmsSender.SENT.isEmpty());
        // 但站内通道照常收到
        assertEquals(List.of("公告"), RecordingChannel.DELIVERED);
    }

    // ---------- 测试用通道与 SPI 实现 ----------

    /// 记录型通道：断言分发是否到达。用静态集合——CDI 客户端代理只代理方法调用，
    /// 从测试侧读实例字段会拿到代理自身的副本。
    @ApplicationScoped
    public static class RecordingChannel implements NotificationChannel {
        static final List<String> DELIVERED = new CopyOnWriteArrayList<>();

        @Override
        public String name() {
            return "recording";
        }

        @Override
        public void deliver(Notification notification) {
            DELIVERED.add(notification.title);
        }
    }

    /// 总是失败的通道：验证单个通道异常不影响其他通道与主流程。
    @ApplicationScoped
    public static class FailingChannel implements NotificationChannel {
        @Override
        public String name() {
            return "failing";
        }

        @Override
        public void deliver(Notification notification) {
            throw new IllegalStateException("boom");
        }
    }

    /// 测试联系方式解析：只有 alice 有邮箱和手机号。
    @ApplicationScoped
    public static class TestContactResolver implements NotificationContactResolver {
        @Override
        public Optional<String> emailOf(String userId) {
            return "alice".equals(userId) ? Optional.of("alice@example.com") : Optional.empty();
        }

        @Override
        public Optional<String> phoneOf(String userId) {
            return "alice".equals(userId) ? Optional.of("13800000000") : Optional.empty();
        }
    }

    /// 记录型短信发送器（业务方 SmsSender 实现的测试替身）。
    @ApplicationScoped
    public static class RecordingSmsSender implements SmsSender {
        static final List<String> SENT = new CopyOnWriteArrayList<>();

        @Override
        public void send(String phone, String text) {
            SENT.add(phone + "|" + text);
        }
    }
}
