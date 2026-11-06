package io.github.foundry4j.quarkforge.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/// ## LocalMessageBus
/// 进程内消息总线：[LocalMessagePublisher] 发布、
/// `LocalMessageSubscriber` 订阅，二者经本总线解耦。
///
/// 分发是同步的（在发布线程内调用处理器），保证本地开发与测试的确定性；
/// 单个处理器抛异常只记录日志，不影响同一 topic 的其他消费者。
@ApplicationScoped
public class LocalMessageBus {

    private static final Logger LOG = Logger.getLogger(LocalMessageBus.class);

    private final Map<String, List<Consumer<MessageEnvelope>>> subscribers = new ConcurrentHashMap<>();

    /// 注册一个 topic 处理器。
    public void subscribe(String topic, Consumer<MessageEnvelope> handler) {
        subscribers.computeIfAbsent(topic, _ -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /// 某 topic 的订阅数，主要用于测试断言。
    public int subscriberCount(String topic) {
        List<Consumer<MessageEnvelope>> handlers = subscribers.get(topic);
        return handlers == null ? 0 : handlers.size();
    }

    /// 同步分发给该 topic 的全部处理器。
    public void dispatch(MessageEnvelope envelope) {
        List<Consumer<MessageEnvelope>> handlers = subscribers.get(envelope.destination());
        if (handlers == null || handlers.isEmpty()) {
            return;
        }
        for (Consumer<MessageEnvelope> handler : handlers) {
            try {
                handler.accept(envelope);
            } catch (Exception e) {
                LOG.warnf(e, "本地消息处理失败 destination=%s messageId=%s",
                        envelope.destination(), envelope.messageId());
            }
        }
    }
}
