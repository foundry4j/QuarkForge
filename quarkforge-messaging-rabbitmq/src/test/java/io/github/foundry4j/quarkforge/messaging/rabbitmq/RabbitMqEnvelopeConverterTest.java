package io.github.foundry4j.quarkforge.messaging.rabbitmq;

import com.rabbitmq.client.AMQP;
import io.github.foundry4j.quarkforge.messaging.MessageEnvelope;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// ## RabbitMqEnvelopeConverterTest
/// 验证 RabbitMQ 投递 → 消息信封的还原转换（发布侧 toProperties 的逆操作）。
class RabbitMqEnvelopeConverterTest {

    @Test
    void convertsFullProperties() {
        Instant occurredAt = Instant.parse("2026-11-06T08:00:00Z");
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .messageId("m-1")
                .timestamp(Date.from(occurredAt))
                .headers(Map.of("tenant", "t1"))
                .build();

        MessageEnvelope envelope = RabbitMqMessages.toEnvelope(
                "order.created", "{\"orderNo\":\"A001\"}".getBytes(StandardCharsets.UTF_8), props);

        assertEquals("m-1", envelope.messageId());
        assertEquals("order.created", envelope.destination());
        assertEquals("{\"orderNo\":\"A001\"}", envelope.payload());
        assertEquals("t1", envelope.headers().get("tenant"));
        assertEquals(occurredAt, envelope.occurredAt());
    }

    @Test
    void fillsDefaultsWhenPropertiesMissing() {
        MessageEnvelope envelope = RabbitMqMessages.toEnvelope("order.created", null, null);

        assertNotNull(envelope.messageId());
        assertEquals("order.created", envelope.destination());
        assertEquals("", envelope.payload());
        assertEquals(Map.of(), envelope.headers());
        assertNotNull(envelope.occurredAt());
    }

    @Test
    void roundTripKeepsIdentity() {
        MessageEnvelope original = MessageEnvelope.of("order.created", "{\"a\":1}", Map.of("k", "v"));
        AMQP.BasicProperties props = RabbitMqMessages.toProperties(original);

        MessageEnvelope restored = RabbitMqMessages.toEnvelope(
                original.destination(), original.payload().getBytes(StandardCharsets.UTF_8), props);

        assertEquals(original.messageId(), restored.messageId());
        assertEquals(original.payload(), restored.payload());
        assertEquals(original.headers(), restored.headers());
    }
}
