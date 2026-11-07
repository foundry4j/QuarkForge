package io.github.foundry4j.quarkforge.notification.channel;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/// ## NotificationContactResolver
/// 联系方式解析 SPI：通知框架不知道业务侧的用户模型，
/// 邮件 / 短信通道通过此接口把 `recipientId`（即 `LoginUser.id()`）解析为联系方式。
///
/// 业务方注册一个实现 Bean 即可（通常查用户表）：
///
/// ```java
/// @ApplicationScoped
/// public class UserContactResolver implements NotificationContactResolver {
///     @Inject UserRepository users;
///     public Optional<String> emailOf(String userId) { return users.findEmail(userId); }
///     public Optional<String> phoneOf(String userId) { return users.findPhone(userId); }
/// }
/// ```
///
/// 未提供实现时使用默认空实现：所有联系方式均缺失，邮件 / 短信通道自动跳过。
public interface NotificationContactResolver {

    /// 解析用户邮箱，查不到返回 `Optional.empty()`。
    Optional<String> emailOf(String userId);

    /// 解析用户手机号，查不到返回 `Optional.empty()`。
    Optional<String> phoneOf(String userId);

    /// 默认实现：不解析任何联系方式。
    @ApplicationScoped
    @DefaultBean
    class Noop implements NotificationContactResolver {
        @Override
        public Optional<String> emailOf(String userId) {
            return Optional.empty();
        }

        @Override
        public Optional<String> phoneOf(String userId) {
            return Optional.empty();
        }
    }
}
