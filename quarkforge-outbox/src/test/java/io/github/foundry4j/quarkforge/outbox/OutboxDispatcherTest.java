package io.github.foundry4j.quarkforge.outbox;

import io.github.foundry4j.quarkforge.messaging.MessageEnvelope;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class OutboxDispatcherTest {

    @Inject
    OutboxService outboxService;

    @Inject
    OutboxDispatcher dispatcher;

    @Inject
    RecordingMessagePublisher publisher;

    @Inject
    OutboxTestCleaner cleaner;

    @BeforeEach
    void reset() {
        cleaner.clear();
        publisher.reset();
    }

    @Test
    void publishesPendingMessageAndMarksSent() {
        Long id = outboxService.enqueue("order.created", Map.of("orderNo", "A001"));

        assertEquals(1, dispatcher.dispatchOnce());

        OutboxMessage message = cleaner.reload(id);
        assertEquals(OutboxStatus.SENT, message.status);
        assertEquals(1, message.attempts);
        assertNotNull(message.sentAt);
        assertEquals(1, publisher.published().size());
        assertEquals("order.created", publisher.published().getFirst().destination());
        assertTrue(publisher.published().getFirst().payload().contains("A001"));
    }

    @Test
    void retriesAfterFailureWithBackoffThenSucceeds() {
        publisher.setFail(true);
        Long id = outboxService.enqueue("order.created", Map.of("orderNo", "A002"));

        assertEquals(0, dispatcher.dispatchOnce());

        OutboxMessage failed = cleaner.reload(id);
        assertEquals(OutboxStatus.PENDING, failed.status);
        assertEquals(1, failed.attempts);
        assertNotNull(failed.lastError);
        assertTrue(failed.nextAttemptAt.isAfter(Instant.now()));

        publisher.setFail(false);
        assertEquals(1, cleaner.makeDue(id));
        assertEquals(1, dispatcher.dispatchOnce());
        assertEquals(1, publisher.published().size());

        OutboxMessage sent = cleaner.reload(id);
        assertEquals(OutboxStatus.SENT, sent.status);
        assertEquals(2, sent.attempts);
    }

    @Test
    void givesUpAfterMaxAttempts() {
        publisher.setFail(true);
        Long id = outboxService.enqueue("order.created", Map.of("orderNo", "A003"));

        for (int i = 0; i < 5; i++) {
            cleaner.makeDue(id);
            dispatcher.dispatchOnce();
        }

        OutboxMessage message = cleaner.reload(id);
        assertEquals(OutboxStatus.FAILED, message.status);
        assertEquals(5, message.attempts);
    }

    @Test
    void skipsDelayedMessage() {
        Long id = outboxService.enqueue("order.close-check", Map.of("orderNo", "A004"), Duration.ofMinutes(10));

        assertEquals(0, dispatcher.dispatchOnce());

        OutboxMessage message = cleaner.reload(id);
        assertEquals(OutboxStatus.PENDING, message.status);
        assertEquals(0, message.attempts);
        assertTrue(publisher.published().isEmpty());
    }

    @Test
    void preservesEnvelopeIdAndHeaders() {
        MessageEnvelope envelope = new MessageEnvelope(
                "msg-42", "order.paid", "{\"orderNo\":\"A005\"}", Map.of("traceId", "t-1"), Instant.now());

        outboxService.enqueue(envelope, Duration.ZERO);
        assertEquals(1, dispatcher.dispatchOnce());

        MessageEnvelope published = publisher.published().getFirst();
        assertEquals("msg-42", published.messageId());
        assertEquals("t-1", published.headers().get("traceId"));
    }

    @Test
    void reclaimsStaleSendingMessage() {
        Long id = outboxService.enqueue("order.created", Map.of("orderNo", "A006"));
        cleaner.markStaleSending(id);

        assertEquals(1, dispatcher.dispatchOnce());

        OutboxMessage message = cleaner.reload(id);
        assertEquals(OutboxStatus.SENT, message.status);
        assertEquals(1, message.attempts);
    }
}
