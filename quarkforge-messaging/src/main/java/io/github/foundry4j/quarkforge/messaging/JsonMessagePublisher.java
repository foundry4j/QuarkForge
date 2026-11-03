package io.github.foundry4j.quarkforge.messaging;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/// ## JsonMessagePublisher
/// 在 [MessagePublisher] 之上提供对象入参的便捷门面，负责把业务对象序列化为 JSON。
///
/// ```java
/// @Inject JsonMessagePublisher publisher;
///
/// publisher.publish("order.created", Map.of("orderNo", orderNo));
/// ```
@ApplicationScoped
public class JsonMessagePublisher {

    private final MessagePublisher publisher;
    private final ObjectMapper objectMapper;

    @Inject
    public JsonMessagePublisher(MessagePublisher publisher, ObjectMapper objectMapper) {
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    /// 序列化对象并投递。
    public CompletionStage<Void> publish(String destination, Object payload) {
        return publish(destination, payload, Map.of());
    }

    /// 序列化对象并携带自定义消息头投递。
    public CompletionStage<Void> publish(String destination, Object payload, Map<String, String> headers) {
        return publisher.publish(MessageEnvelope.of(destination, toJson(payload), headers));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("消息负载序列化失败", e);
        }
    }
}
