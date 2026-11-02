package io.github.tuyucheng777.quarkforge.outbox;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import io.quarkus.hibernate.orm.panache.Panache;

/// 测试辅助：清理数据、调整消息状态，均需要事务。
@ApplicationScoped
public class OutboxTestCleaner {

    @Transactional
    public void clear() {
        OutboxMessage.deleteAll();
    }

    @Transactional
    public int makeDue(Long id) {
        int updated = OutboxMessage.update("nextAttemptAt = ?1 where id = ?2", Instant.now().minusSeconds(1), id);
        Panache.getEntityManager(OutboxMessage.class).clear();
        return updated;
    }

    /// 绕过一级缓存读取最新状态。
    @Transactional
    public OutboxMessage reload(Long id) {
        Panache.getEntityManager(OutboxMessage.class).clear();
        return OutboxMessage.findById(id);
    }

    @Transactional
    public void markStaleSending(Long id) {
        OutboxMessage.update("status = ?1, updatedAt = ?2 where id = ?3",
                OutboxStatus.SENDING, Instant.now().minusSeconds(600), id);
        Panache.getEntityManager(OutboxMessage.class).clear();
    }
}
