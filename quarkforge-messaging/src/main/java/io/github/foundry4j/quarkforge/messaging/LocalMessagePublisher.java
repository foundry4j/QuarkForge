package io.github.foundry4j.quarkforge.messaging;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/// ## LocalMessagePublisher
/// 单机默认实现：不连接任何中间件，只把消息写入内存环形缓冲并打印日志。
///
/// 适合本地开发、单元测试，或尚未接入 MQ 的服务；接入
/// `quarkforge-messaging-rabbitmq` 后会自动被 RabbitMQ 实现覆盖。
@DefaultBean
@ApplicationScoped
public class LocalMessagePublisher implements MessagePublisher {

    private static final Logger LOG = Logger.getLogger(LocalMessagePublisher.class);
    private static final int MAX_HISTORY = 100;

    private final Deque<MessageEnvelope> history = new ArrayDeque<>(MAX_HISTORY);

    @Inject
    LocalMessageBus bus;

    @Override
    public CompletionStage<Void> publish(MessageEnvelope message) {
        synchronized (history) {
            if (history.size() >= MAX_HISTORY) {
                history.removeFirst();
            }
            history.addLast(message);
        }
        LOG.debugf("本地投递消息 destination=%s messageId=%s", message.destination(), message.messageId());
        // 同步分发给本进程内订阅该 destination 的消费者；null 判定允许脱离 CDI 直接 new 使用（单元测试）
        if (bus != null) {
            bus.dispatch(message);
        }
        return CompletableFuture.completedFuture(null);
    }

    /// 最近发布的消息快照（最多 100 条），便于本地排查与测试断言。
    public List<MessageEnvelope> history() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    /// 清空历史记录。
    public void clear() {
        synchronized (history) {
            history.clear();
        }
    }
}
