package io.github.foundry4j.quarkforge.notification.channel;

import io.github.foundry4j.quarkforge.notification.entity.Notification;
import io.github.foundry4j.quarkforge.notification.sms.SmsSender;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/// ## SmsNotificationChannel
/// 短信通道，设置 `quarkforge.notification.sms.enabled=true` 后启用。
///
/// - 具体发送委托给业务方实现的 [SmsSender] Bean（对接阿里云/腾讯云等厂商 SDK）；
///   容器中没有 `SmsSender` Bean 时通道静默跳过
/// - 手机号由 [NotificationContactResolver] 解析，解析不到则跳过
@ApplicationScoped
@IfBuildProperty(name = "quarkforge.notification.sms.enabled", stringValue = "true")
public class SmsNotificationChannel implements NotificationChannel {

    @Inject
    Instance<SmsSender> sender;

    @Inject
    NotificationContactResolver contacts;

    @Override
    public String name() {
        return "sms";
    }

    @Override
    public void deliver(Notification notification) {
        if (sender.isUnsatisfied()) {
            Log.debug("短信通道已启用但未发现 SmsSender 实现 Bean，跳过");
            return;
        }
        String phone = contacts.phoneOf(notification.recipientId).orElse(null);
        if (phone == null || phone.isBlank()) {
            Log.debugf("通知 %d 的接收人 %s 未配置手机号，短信通道跳过",
                    notification.id, notification.recipientId);
            return;
        }
        StringBuilder text = new StringBuilder(notification.title);
        if (notification.content != null && !notification.content.isBlank()) {
            text.append("：").append(notification.content);
        }
        sender.get().send(phone, text.toString());
    }
}
