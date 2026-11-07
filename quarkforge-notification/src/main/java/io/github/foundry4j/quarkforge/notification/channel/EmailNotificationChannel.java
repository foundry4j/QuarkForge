package io.github.foundry4j.quarkforge.notification.channel;

import io.github.foundry4j.quarkforge.notification.entity.Notification;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/// ## EmailNotificationChannel
/// 邮件通道，设置 `quarkforge.notification.email.enabled=true` 后启用。
///
/// - 收件人邮箱由 [NotificationContactResolver] 解析，解析不到则跳过
/// - 邮件标题用通知 title，正文用 content（无正文时退化为 title）
/// - 发件人等参数走 Quarkus Mailer 标准配置（`quarkus.mailer.from` 等）
@ApplicationScoped
@IfBuildProperty(name = "quarkforge.notification.email.enabled", stringValue = "true")
public class EmailNotificationChannel implements NotificationChannel {

    @Inject
    Mailer mailer;

    @Inject
    NotificationContactResolver contacts;

    @Override
    public String name() {
        return "email";
    }

    @Override
    public void deliver(Notification notification) {
        String address = contacts.emailOf(notification.recipientId).orElse(null);
        if (address == null || address.isBlank()) {
            Log.debugf("通知 %d 的接收人 %s 未配置邮箱，邮件通道跳过",
                    notification.id, notification.recipientId);
            return;
        }
        String body = notification.content != null && !notification.content.isBlank()
                ? notification.content
                : notification.title;
        mailer.send(Mail.withText(address, notification.title, body));
    }
}
