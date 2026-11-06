package io.github.foundry4j.quarkforge.messaging;

/// ## MessageHandler
/// 消息处理接口。实现类标注 [MessageConsumer] 后由框架在启动时自动订阅，
/// 负载已按 `payloadType` 反序列化后传入。
///
/// @param <T> 负载类型
public interface MessageHandler<T> {

    /// 处理一条消息。
    ///
    /// 抛出异常时由订阅实现决定后续：本地总线记录日志后继续其他消费者；
    /// RabbitMQ 返回 `nack`（不入队重投），避免毒消息反复投递阻塞队列。
    ///
    /// @param payload  反序列化后的业务负载
    /// @param envelope 消息信封（messageId 可用于消费端幂等）
    void onMessage(T payload, MessageEnvelope envelope);
}
