package io.github.tuyucheng777.quarkforge.outbox;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import io.github.tuyucheng777.quarkforge.messaging.MessageEnvelope;
import io.github.tuyucheng777.quarkforge.messaging.MessagePublisher;

/// 测试用发布器：记录消息，可切换为失败模式，覆盖默认的 [io.github.tuyucheng777.quarkforge.messaging.LocalMessagePublisher]。
///
/// 注意：CDI 客户端代理不会同步字段值，测试必须通过方法访问状态。
@ApplicationScoped
public class RecordingMessagePublisher implements MessagePublisher {

    private final List<MessageEnvelope> published = new CopyOnWriteArrayList<>();

    private volatile boolean fail;

    @Override
    public CompletionStage<Void> publish(MessageEnvelope message) {
        if (fail) {
            return CompletableFuture.failedFuture(new IllegalStateException("模拟 MQ 不可用"));
        }
        published.add(message);
        return CompletableFuture.completedFuture(null);
    }

    public List<MessageEnvelope> published() {
        return published;
    }

    public void setFail(boolean fail) {
        this.fail = fail;
    }

    public void reset() {
        published.clear();
        fail = false;
    }
}
