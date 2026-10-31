package io.github.tuyucheng777.quarkforge.idempotency;

import io.quarkus.arc.DefaultBean;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/// ## InMemoryIdempotentStore
/// 基于内存的默认幂等存储，适用于单机部署。
///
/// - 使用 `ConcurrentHashMap` 保存状态，`tryAcquire` 通过 `compute` 保证原子性；
/// - 过期条目在访问时惰性回收，并由单线程调度器定期清理，避免内存无限增长；
/// - 多实例部署请引入 `quarkforge-redis` 模块，自动切换为分布式实现。
@DefaultBean
@ApplicationScoped
public class InMemoryIdempotentStore implements IdempotentStore {

    private final Map<String, IdempotentEntry> entries = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "quarkforge-idempotent-cleaner");
        t.setDaemon(true);
        return t;
    });

    public InMemoryIdempotentStore() {
        cleaner.scheduleAtFixedRate(this::evictExpired, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    public IdempotentEntry tryAcquire(String key, long ttlSeconds) {
        long now = System.currentTimeMillis();
        long ttlMillis = ttlSeconds * 1000L;
        IdempotentEntry[] fresh = new IdempotentEntry[1];
        IdempotentEntry existing = entries.compute(key, (_, current) -> {
            if (current == null || now >= current.expiresAt()) {
                IdempotentEntry created = new IdempotentEntry(IdempotentEntry.State.PROCESSING, null, now + ttlMillis);
                fresh[0] = created;
                return created;
            }
            return current;
        });
        return fresh[0] == existing ? null : existing;
    }

    @Override
    public void markDone(String key, byte[] payload, long ttlSeconds) {
        long expiresAt = System.currentTimeMillis() + ttlSeconds * 1000L;
        entries.put(key, new IdempotentEntry(IdempotentEntry.State.DONE, payload, expiresAt));
    }

    @Override
    public void markFailed(String key) {
        entries.remove(key);
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        entries.entrySet().removeIf(e -> now >= e.getValue().expiresAt());
    }

    @PreDestroy
    void shutdown() {
        cleaner.shutdownNow();
    }
}
