package io.github.foundry4j.quarkforge.messaging.rabbitmq;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BasicProperties;
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

    /// 将 RabbitMQ 投递还原为消息信封，供消费端处理器使用。
    /// `messageId` / `occurredAt` 缺失时由信封构造器补默认值。
    public static MessageEnvelope toEnvelope(String destination, byte[] body, BasicProperties properties) {
        String messageId = null;
        Map<String, String> headers = Map.of();
        Instant occurredAt = null;
        if (properties != null) {
            messageId = properties.getMessageId();
            if (properties.getHeaders() != null) {
                headers = new HashMap<>();
                Map<String, String> copy = headers;
                properties.getHeaders().forEach((k, v) -> copy.put(k, String.valueOf(v)));
            }
            if (properties.getTimestamp() != null) {
                occurredAt = properties.getTimestamp().toInstant();
            }
        }
        String payload = body == null ? "" : new String(body, StandardCharsets.UTF_8);
        return new MessageEnvelope(messageId, destination, payload, headers, occurredAt);
    }
}
