package io.github.foundry4j.quarkforge.messaging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## MessageConsumer
/// 声明一个消息消费者。实现 [MessageHandler] 接口并加本注解，
/// 应用启动时会自动通过 [MessageSubscriber] 订阅对应 `topic`：
///
/// ```java
/// @MessageConsumer(topic = "order.created", payloadType = OrderCreated.class)
/// public class OrderCreatedConsumer implements MessageHandler<OrderCreated> {
///
///     @Override
///     public void onMessage(OrderCreated payload, MessageEnvelope envelope) {
///         // 业务处理；envelope 提供 messageId / headers / occurredAt 供幂等与追踪
///     }
/// }
/// ```
///
/// 消费端不感知中间件：本地开发由 `LocalMessageBus` 同进程分发，
/// 引入 `quarkforge-messaging-rabbitmq` 后自动切换为 RabbitMQ 消费（手动 ack）。
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageConsumer {

    /// 订阅的目标地址，与发布侧 `destination` 对应。
    String topic();

    /// 负载反序列化目标类型；默认 `String` 表示消费原始 JSON 文本。
    Class<?> payloadType() default String.class;
}
