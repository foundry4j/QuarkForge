package io.github.tuyucheng777.quarkforge.messaging.rabbitmq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.rabbitmq.client.AMQP;

import io.github.tuyucheng777.quarkforge.messaging.MessageEnvelope;

class RabbitMqMessagesTest {

    @Test
    void mapsEnvelopeToBasicProperties() {
        Instant occurredAt = Instant.parse("2026-01-01T00:00:00Z");
        MessageEnvelope envelope = new MessageEnvelope(
                "msg-1", "order.created", "{\"orderNo\":\"A001\"}", Map.of("traceId", "t-1"), occurredAt);

        AMQP.BasicProperties properties = RabbitMqMessages.toProperties(envelope);

        assertEquals("msg-1", properties.getMessageId());
        assertEquals("application/json", properties.getContentType());
        assertEquals("UTF-8", properties.getContentEncoding());
        assertEquals(Integer.valueOf(2), properties.getDeliveryMode());
        assertEquals(Date.from(occurredAt), properties.getTimestamp());
        assertEquals("t-1", properties.getHeaders().get("traceId"));
    }

    @Test
    void omitsHeadersWhenEmpty() {
        AMQP.BasicProperties properties = RabbitMqMessages.toProperties(
                MessageEnvelope.of("order.created", "{}"));

        assertNull(properties.getHeaders());
    }
}
