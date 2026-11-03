package io.github.foundry4j.quarkforge.messaging;

import java.util.concurrent.CompletionStage;

/// ## MessagePublisher
/// 消息发布 SPI。默认由 [LocalMessagePublisher] 承接（仅记录日志），
/// 引入 `quarkforge-messaging-rabbitmq` 后自动切换为 RabbitMQ 投递。
///
/// 返回 [CompletionStage]，调用方可以选择等待投递结果（如事务发件箱）
/// 或完全异步（普通业务通知）。
public interface MessagePublisher {

    /// 投递一条消息。
    ///
    /// @param message 消息信封
    /// @return 完成表示已成功交付给中间件
    CompletionStage<Void> publish(MessageEnvelope message);

    /// 投递 JSON 负载的便捷方法。
    default CompletionStage<Void> publish(String destination, String payload) {
        return publish(MessageEnvelope.of(destination, payload));
    }
}
