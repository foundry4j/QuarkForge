package io.github.foundry4j.quarkforge.notification.channel;

import io.github.foundry4j.quarkforge.notification.entity.Notification;

/// ## NotificationChannel
/// 通知送达通道 SPI。通知持久化后，框架会依次调用容器中所有通道 Bean 的
/// [#deliver(Notification)]，单个通道抛异常不会影响其他通道。
///
/// 内置通道：
///
/// | 通道          | 启用条件                                    |
/// |---------------|---------------------------------------------|
/// | `websocket`   | 始终启用（站内实时推送，离线重连补偿）       |
/// | `email`       | `quarkforge.notification.email.enabled=true` |
/// | `sms`         | `quarkforge.notification.sms.enabled=true` 且存在 `SmsSender` Bean |
///
/// 业务方自定义通道（如钉钉、企业微信机器人）只需注册一个实现 Bean：
///
/// ```java
/// @ApplicationScoped
/// public class DingTalkChannel implements NotificationChannel {
///     public String name() { return "dingtalk"; }
///     public void deliver(Notification n) { ... }
/// }
/// ```
public interface NotificationChannel {

    /// 通道名，用于日志与排查。
    String name();

    /// 投递一条已持久化的通知。实现应自行处理"接收人联系方式缺失"等场景并静默跳过，
    /// 抛出异常只会被框架记录为警告，不影响其他通道与主流程。
    void deliver(Notification notification);
}
