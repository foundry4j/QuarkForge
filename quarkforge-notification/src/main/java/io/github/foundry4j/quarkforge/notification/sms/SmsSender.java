package io.github.foundry4j.quarkforge.notification.sms;

/// ## SmsSender
/// 短信发送 SPI：框架只定义抽象，不绑定具体厂商。
/// 业务方用厂商 SDK（阿里云、腾讯云等）实现并注册为 CDI Bean，
/// 短信通道（`quarkforge.notification.sms.enabled=true`）发现该 Bean 后才会启用。
///
/// ```java
/// @ApplicationScoped
/// public class AliyunSmsSender implements SmsSender {
///     @Override
///     public void send(String phone, String text) {
///         // 调厂商 SDK 发送
///     }
/// }
/// ```
public interface SmsSender {

    /// 发送一条短信。
    ///
    /// @param phone 手机号（由 `NotificationContactResolver#phoneOf` 解析）
    /// @param text  文本内容（标题 + 正文拼接）
    void send(String phone, String text);
}
