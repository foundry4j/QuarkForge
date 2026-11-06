package io.github.foundry4j.quarkforge.messaging;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/// ## LocalMessageSubscriber
/// [MessageSubscriber] 的单机默认实现：订阅登记到 [LocalMessageBus]，
/// 由同进程的 [LocalMessagePublisher] 在发布时同步分发。
/// 引入 `quarkforge-messaging-rabbitmq` 后本实现被自动覆盖。
@DefaultBean
@ApplicationScoped
public class LocalMessageSubscriber implements MessageSubscriber {

    @Inject
    LocalMessageBus bus;

    @Override
    public CompletionStage<Void> subscribe(String topic, Consumer<MessageEnvelope> handler) {
        bus.subscribe(topic, handler);
        return CompletableFuture.completedFuture(null);
    }
}
