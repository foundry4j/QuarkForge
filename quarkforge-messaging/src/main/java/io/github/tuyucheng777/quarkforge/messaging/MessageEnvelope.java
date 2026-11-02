package io.github.tuyucheng777.quarkforge.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/// ## MessageEnvelope
/// 与具体消息中间件无关的消息信封。
///
/// - `messageId`：消息唯一标识，消费端可据此做幂等
/// - `destination`：目标地址，RabbitMQ 中作为 routing key（或队列名）
/// - `payload`：JSON 字符串，框架不关心具体序列化方式
/// - `headers`：附加消息头，均为字符串，避免不同中间件的类型差异
///
/// 创建方式：
///
/// ```java
/// MessageEnvelope envelope = MessageEnvelope.of("order.created", "{\"orderNo\":\"A001\"}");
/// ```
public record MessageEnvelope(
        String messageId,
        String destination,
        String payload,
        Map<String, String> headers,
        Instant occurredAt) {

    public MessageEnvelope {
        messageId = (messageId == null || messageId.isBlank()) ? UUID.randomUUID().toString() : messageId;
        Objects.requireNonNull(destination, "destination 不能为空");
        Objects.requireNonNull(payload, "payload 不能为空");
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    /// 使用随机消息 ID 与当前时间创建消息。
    public static MessageEnvelope of(String destination, String payload) {
        return new MessageEnvelope(null, destination, payload, Map.of(), Instant.now());
    }

    /// 带自定义消息头创建消息。
    public static MessageEnvelope of(String destination, String payload, Map<String, String> headers) {
        return new MessageEnvelope(null, destination, payload, headers, Instant.now());
    }

    /// 复制一条消息并追加一个消息头。
    public MessageEnvelope withHeader(String name, String value) {
        Map<String, String> merged = new java.util.HashMap<>(headers);
        merged.put(name, value);
        return new MessageEnvelope(messageId, destination, payload, merged, occurredAt);
    }
}
