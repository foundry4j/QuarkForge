package io.github.foundry4j.quarkforge.outbox;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/// ## OutboxPersistence
/// 发件箱状态迁移的事务边界。
///
/// 每条消息的「认领 → 投递 → 标记结果」都拆在独立事务里：
/// 投递本身不占用数据库事务，避免 MQ 抖动拖长业务事务；
/// 认领使用条件更新，多实例部署时同一条消息只会被一个实例抢到。
@ApplicationScoped
public class OutboxPersistence {

    @Inject
    OutboxRepository repository;

    /// 原子认领一条可投递消息，并把尝试次数加一。
    ///
    /// @return 认领成功返回消息，否则返回 `null`
    @Transactional
    public OutboxMessage claimNext(Instant now, Instant staleBefore) {
        OutboxMessage candidate = repository.findNextDispatchable(now, staleBefore).orElse(null);
        if (candidate == null) {
            return null;
        }
        int claimed = repository.update(
                "status = ?1, attempts = attempts + 1, updatedAt = ?2 "
                        + "where id = ?3 and (status = ?4 or (status = ?5 and updatedAt <= ?6))",
                OutboxStatus.SENDING, now, candidate.id,
                OutboxStatus.PENDING, OutboxStatus.SENDING, staleBefore);
        if (claimed == 0) {
            return null;
        }
        candidate.status = OutboxStatus.SENDING;
        candidate.attempts = candidate.attempts + 1;
        candidate.updatedAt = now;
        return candidate;
    }

    /// 标记投递成功。
    @Transactional
    public void markSent(Long id) {
        repository.update("status = ?1, sentAt = ?2, lastError = null, updatedAt = ?3 where id = ?4",
                OutboxStatus.SENT, Instant.now(), Instant.now(), id);
    }

    /// 标记投递失败：未达上限则回到 `PENDING` 等待退避重试，否则置为 `FAILED`。
    @Transactional
    public void markFailed(Long id, int attempts, Instant retryAt, int maxAttempts, String error) {
        if (attempts >= maxAttempts) {
            repository.update("status = ?1, lastError = ?2, updatedAt = ?3 where id = ?4",
                    OutboxStatus.FAILED, error, Instant.now(), id);
        } else {
            repository.update("status = ?1, lastError = ?2, nextAttemptAt = ?3, updatedAt = ?4 where id = ?5",
                    OutboxStatus.PENDING, error, retryAt, Instant.now(), id);
        }
    }
}
