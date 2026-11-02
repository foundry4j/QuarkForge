package io.github.tuyucheng777.quarkforge.outbox;

import java.time.Instant;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import io.github.tuyucheng777.quarkforge.data.BaseRepository;

/// ## OutboxRepository
/// 发件箱消息仓储。可投递条件：
///
/// - `PENDING` 且已到 `nextAttemptAt`（含延迟消息与失败退避）
/// - `SENDING` 且 `updatedAt` 已超过阈值（上次投递过程中实例崩溃，需要回收）
@ApplicationScoped
public class OutboxRepository extends BaseRepository<OutboxMessage> {

    /// 查询一条可投递消息，按 ID 升序保证先入先出。
    public Optional<OutboxMessage> findNextDispatchable(Instant now, Instant staleBefore) {
        return find("(status = ?1 and nextAttemptAt <= ?2) or (status = ?3 and updatedAt <= ?4) order by id",
                OutboxStatus.PENDING, now, OutboxStatus.SENDING, staleBefore)
                .firstResultOptional();
    }
}
