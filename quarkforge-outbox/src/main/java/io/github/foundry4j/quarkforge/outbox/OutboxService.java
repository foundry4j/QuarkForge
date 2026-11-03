package io.github.foundry4j.quarkforge.outbox;

import java.time.Duration;
import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.foundry4j.quarkforge.messaging.MessageEnvelope;

/// ## OutboxService
/// 事务发件箱写入入口。
///
/// 在业务事务内调用 `enqueue`，消息与业务数据一起提交；即使 MQ 暂时不可用，
/// 已提交的消息也会由 [OutboxDispatcher] 在后台补投，不会丢单。
///
/// ```java
/// @Transactional
/// public void createOrder(CreateOrderRequest request) {
///     orderRepository.persist(order);
///     outbox.enqueue("order.created", new OrderCreatedEvent(order.id, order.no));
/// }
/// ```
///
/// 延迟投递（例如 30 分钟未支付自动关单）：
///
/// ```java
/// outbox.enqueue("order.close-check", Map.of("orderNo", orderNo), Duration.ofMinutes(30));
/// ```
@ApplicationScoped
public class OutboxService {

    @Inject
    OutboxRepository repository;

    @Inject
    ObjectMapper objectMapper;

    /// 立即投递一条消息。
    @Transactional
    public Long enqueue(String destination, Object payload) {
        return enqueue(destination, payload, Duration.ZERO);
    }

    /// 延迟投递一条消息，延迟从当前时间开始计算。
    @Transactional
    public Long enqueue(String destination, Object payload, Duration delay) {
        return persist(destination, null, toJson(payload), null, delay);
    }

    /// 投递一条已有消息信封，保留其消息 ID 与消息头。
    @Transactional
    public Long enqueue(MessageEnvelope envelope, Duration delay) {
        String headerJson = envelope.headers().isEmpty() ? null : toJson(envelope.headers());
        return persist(envelope.destination(), envelope.messageId(), envelope.payload(), headerJson, delay);
    }

    private Long persist(String destination, String messageId, String payload, String headerJson, Duration delay) {
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("消息 destination 不能为空");
        }
        OutboxMessage message = new OutboxMessage();
        message.messageId = messageId;
        message.destination = destination;
        message.payload = payload;
        message.headers = headerJson;
        message.status = OutboxStatus.PENDING;
        message.attempts = 0;
        message.nextAttemptAt = Instant.now().plus(delay == null || delay.isNegative() ? Duration.ZERO : delay);
        repository.persist(message);
        return message.id;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("发件箱消息序列化失败", e);
        }
    }
}
