package io.github.foundry4j.quarkforge.messaging.rabbitmq;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.rabbitmq.client.AMQP;
import io.github.foundry4j.quarkforge.messaging.MessageEnvelope;

/// ## RabbitMqMessages
/// [MessageEnvelope] 与 RabbitMQ 消息属性的转换。
///
/// 统一约定：JSON 负载、UTF-8、持久化投递（delivery mode 2），
/// 消息 ID 与时间戳透传，便于消费端做幂等与链路追踪。
public final class RabbitMqMessages {

    private RabbitMqMessages() {
    }

    /// 构造 RabbitMQ 消息属性。
    public static AMQP.BasicProperties toProperties(MessageEnvelope message) {
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder()
                .messageId(message.messageId())
                .contentType("application/json")
                .contentEncoding("UTF-8")
                .deliveryMode(2)
                .timestamp(Date.from(message.occurredAt()));
        Map<String, String> headers = message.headers();
        if (!headers.isEmpty()) {
            builder.headers(new HashMap<>(headers));
        }
        return builder.build();
    }
}
