package io.github.foundry4j.quarkforge.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalMessagePublisherTest {

    @Test
    void keepsRecentHistory() {
        LocalMessagePublisher publisher = new LocalMessagePublisher();

        publisher.publish("order.created", "{\"orderNo\":\"A001\"}").toCompletableFuture().join();

        assertEquals(1, publisher.history().size());
        MessageEnvelope message = publisher.history().getFirst();
        assertEquals("order.created", message.destination());
        assertTrue(message.payload().contains("A001"));
        assertTrue(message.messageId() != null && !message.messageId().isBlank());
    }

    @Test
    void clearRemovesHistory() {
        LocalMessagePublisher publisher = new LocalMessagePublisher();
        publisher.publish("order.created", "{}").toCompletableFuture().join();

        publisher.clear();

        assertTrue(publisher.history().isEmpty());
    }
}
