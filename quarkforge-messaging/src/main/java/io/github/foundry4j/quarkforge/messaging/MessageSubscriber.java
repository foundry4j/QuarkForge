package io.github.foundry4j.quarkforge.messaging;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/// ## MessageSubscriber
/// 消息订阅 SPI，与 [MessagePublisher] 对称。默认由本地实现
/// （`LocalMessageSubscriber`，同 JVM 分发）承接；引入
/// `quarkforge-messaging-rabbitmq` 后自动切换为 RabbitMQ 消费。
///
/// 框架启动时扫描 `@MessageConsumer` Bean 并注册到这里，
/// 业务代码通常不直接使用本接口。
public interface MessageSubscriber {

    /// 订阅一个目标地址。
    ///
    /// @param topic   目标地址（与发布侧 destination 对应）
    /// @param handler 信封级处理器，反序列化与业务分发由框架包装
    /// @return 订阅建立完成
    CompletionStage<Void> subscribe(String topic, Consumer<MessageEnvelope> handler);
}
