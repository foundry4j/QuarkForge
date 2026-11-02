package io.github.tuyucheng777.quarkforge.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tuyucheng777.quarkforge.messaging.MessageEnvelope;
import io.github.tuyucheng777.quarkforge.messaging.MessagePublisher;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/// ## OutboxDispatcher
/// 发件箱后台投递器，默认每 5 秒扫描一次本地消息表：
///
/// 1. 认领一条到期消息（`SENDING` 超时的消息会被回收重投）
/// 2. 通过 [MessagePublisher] 投递
/// 3. 成功标记 `SENT`；失败按 `backoff-seconds × 2^(attempts-1)` 退避，超过上限标记 `FAILED`
///
/// 配置项：
///
/// ```properties
/// quarkforge.outbox.enabled=true
/// quarkforge.outbox.dispatch-interval=5s
/// quarkforge.outbox.batch-size=20
/// quarkforge.outbox.max-attempts=5
/// quarkforge.outbox.backoff-seconds=30
/// quarkforge.outbox.stale-sending-seconds=300
/// quarkforge.outbox.publish-timeout-seconds=10
/// ```
@ApplicationScoped
public class OutboxDispatcher {

    private static final Logger LOG = Logger.getLogger(OutboxDispatcher.class);
    private static final int MAX_BACKOFF_SECONDS = 3600;
    private static final int MAX_ERROR_LENGTH = 1000;

    @Inject
    OutboxPersistence persistence;

    @Inject
    MessagePublisher publisher;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "quarkforge.outbox.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "quarkforge.outbox.batch-size", defaultValue = "20")
    int batchSize;

    @ConfigProperty(name = "quarkforge.outbox.stale-sending-seconds", defaultValue = "300")
    long staleSendingSeconds;

    @ConfigProperty(name = "quarkforge.outbox.backoff-seconds", defaultValue = "30")
    long backoffSeconds;

    @ConfigProperty(name = "quarkforge.outbox.max-attempts", defaultValue = "5")
    int maxAttempts;

    @ConfigProperty(name = "quarkforge.outbox.publish-timeout-seconds", defaultValue = "10")
    long publishTimeoutSeconds;

    /// 定时扫描入口，`quarkforge.outbox.enabled=false` 时跳过。
    @Scheduled(every = "${quarkforge.outbox.dispatch-interval:5s}", delayed = "1s",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Blocking
    void scheduledDispatch() {
        if (enabled) {
            dispatchOnce();
        }
    }

    /// 立即执行一轮投递（测试或手动补偿时可调用）。
    ///
    /// @return 本轮成功投递的消息条数
    public int dispatchOnce() {
        Instant now = Instant.now();
        Instant staleBefore = now.minusSeconds(staleSendingSeconds);
        int sent = 0;
        for (int i = 0; i < batchSize; i++) {
            OutboxMessage message = persistence.claimNext(now, staleBefore);
            if (message == null) {
                break;
            }
            if (deliver(message)) {
                sent++;
            }
        }
        return sent;
    }

    private boolean deliver(OutboxMessage message) {
        try {
            publisher.publish(toEnvelope(message))
                    .toCompletableFuture()
                    .get(publishTimeoutSeconds, TimeUnit.SECONDS);
            persistence.markSent(message.id);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            persistence.markFailed(message.id, message.attempts, nextAttemptAt(message.attempts), maxAttempts,
                    "投递被中断");
        } catch (Exception e) {
            String error = truncate(rootMessage(e));
            LOG.warnf(e, "发件箱消息投递失败 id=%s destination=%s attempts=%s",
                    message.id, message.destination, message.attempts);
            persistence.markFailed(message.id, message.attempts, nextAttemptAt(message.attempts), maxAttempts, error);
        }
        return false;
    }

    private MessageEnvelope toEnvelope(OutboxMessage message) throws Exception {
        Map<String, String> headers = message.headers == null
                ? Map.of()
                : objectMapper.readValue(message.headers, new TypeReference<>() {
        });
        return new MessageEnvelope(
                message.messageId,
                message.destination,
                message.payload,
                headers,
                message.createdAt == null ? Instant.now() : message.createdAt);
    }

    /// 指数退避：首次失败等待 `backoff-seconds`，之后逐次翻倍，最长 1 小时。
    private Instant nextAttemptAt(int attempts) {
        int exponent = Math.clamp(attempts - 1, 0, 10);
        long seconds = Math.min(backoffSeconds * (1L << exponent), MAX_BACKOFF_SECONDS);
        return Instant.now().plusSeconds(seconds);
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private String truncate(String value) {
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
