package io.github.foundry4j.quarkforge.messaging;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// ## MessageConsumerInitializerTest
/// 本地总线上的消费闭环：`@MessageConsumer` Bean 启动时自动订阅，
/// 发布后经总线同步分发并完成负载反序列化。
@QuarkusTest
class MessageConsumerInitializerTest {

    @Inject
    JsonMessagePublisher publisher;

    // ---------- 测试消费者 ----------

    public record OrderPayload(String orderNo) {
    }

    @MessageConsumer(topic = "order.created", payloadType = OrderPayload.class)
    @ApplicationScoped
    public static class TypedRecorder implements MessageHandler<OrderPayload> {

        static final List<OrderPayload> payloads = new CopyOnWriteArrayList<>();
        static final List<MessageEnvelope> envelopes = new CopyOnWriteArrayList<>();

        @Override
        public void onMessage(OrderPayload payload, MessageEnvelope envelope) {
            payloads.add(payload);
            envelopes.add(envelope);
        }
    }

    @MessageConsumer(topic = "order.created")
    @ApplicationScoped
    public static class RawRecorder implements MessageHandler<String> {

        static final List<String> payloads = new CopyOnWriteArrayList<>();

        @Override
        public void onMessage(String payload, MessageEnvelope envelope) {
            payloads.add(payload);
        }
    }

    @MessageConsumer(topic = "order.created")
    @ApplicationScoped
    public static class FailingConsumer implements MessageHandler<String> {

        static int invoked;

        @Override
        public void onMessage(String payload, MessageEnvelope envelope) {
            invoked++;
            throw new IllegalStateException("boom");
        }
    }

    @BeforeEach
    void reset() {
        TypedRecorder.payloads.clear();
        TypedRecorder.envelopes.clear();
        RawRecorder.payloads.clear();
        FailingConsumer.invoked = 0;
    }

    @Test
    void typedConsumerReceivesDeserializedPayload() {
        publisher.publish("order.created", new OrderPayload("A001"), Map.of("tenant", "t1"))
                .toCompletableFuture().join();

        assertEquals(1, TypedRecorder.payloads.size());
        assertEquals("A001", TypedRecorder.payloads.getFirst().orderNo());

        // 信封元数据对业务可见：消息头、消息 ID、目标地址
        MessageEnvelope envelope = TypedRecorder.envelopes.getFirst();
        assertEquals("t1", envelope.headers().get("tenant"));
        assertEquals("order.created", envelope.destination());
        assertNotNull(envelope.messageId());
    }

    @Test
    void rawConsumerReceivesJsonText() {
        publisher.publish("order.created", new OrderPayload("A002")).toCompletableFuture().join();

        assertEquals(1, RawRecorder.payloads.size());
        assertTrue(RawRecorder.payloads.getFirst().contains("\"orderNo\":\"A002\""));
    }

    @Test
    void failingConsumerDoesNotAffectOthers() {
        publisher.publish("order.created", new OrderPayload("A003")).toCompletableFuture().join();

        assertEquals(1, FailingConsumer.invoked);
        assertFalse(TypedRecorder.payloads.isEmpty());
        assertFalse(RawRecorder.payloads.isEmpty());
    }

    @Test
    void consumerOnlyReceivesOwnTopic() {
        publisher.publish("stock.changed", new OrderPayload("S001")).toCompletableFuture().join();

        assertTrue(TypedRecorder.payloads.isEmpty());
        assertTrue(RawRecorder.payloads.isEmpty());
    }
}
