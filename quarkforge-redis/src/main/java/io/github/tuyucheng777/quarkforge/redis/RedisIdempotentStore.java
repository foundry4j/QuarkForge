package io.github.tuyucheng777.quarkforge.redis;

import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.tuyucheng777.quarkforge.idempotency.IdempotentEntry;
import io.github.tuyucheng777.quarkforge.idempotency.IdempotentStore;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

/// ## RedisIdempotentStore
/// 基于 Redis 的分布式幂等存储。
///
/// 以 `SETNX` 原子占用幂等键：设置成功表示首次执行，否则读取已有状态。
/// 引入本模块后优先级高于 idempotency 模块中的内存默认实现，自动生效。
@ApplicationScoped
public class RedisIdempotentStore implements IdempotentStore {

    private static final String PREFIX = "idem:";

    private final ValueCommands<String, String> values;
    private final KeyCommands<String> keys;
    private final ObjectMapper objectMapper;

    @Inject
    public RedisIdempotentStore(RedisDataSource dataSource, ObjectMapper objectMapper) {
        this.values = dataSource.value(String.class);
        this.keys = dataSource.key();
        this.objectMapper = objectMapper;
    }

    @Override
    public IdempotentEntry tryAcquire(String key, long ttlSeconds) {
        String fullKey = PREFIX + key;
        String processing = toJson(new IdempotentEntry(IdempotentEntry.State.PROCESSING, null, 0));
        Boolean created = values.setnx(fullKey, processing);
        if (Boolean.TRUE.equals(created)) {
            keys.expire(fullKey, ttlSeconds);
            return null;
        }
        String stored = values.get(fullKey);
        if (stored == null) {
            return null;
        }
        return fromJson(stored);
    }

    @Override
    public void markDone(String key, byte[] payload, long ttlSeconds) {
        String fullKey = PREFIX + key;
        IdempotentEntry entry = new IdempotentEntry(IdempotentEntry.State.DONE, payload, 0);
        values.setex(fullKey, ttlSeconds, toJson(entry));
    }

    @Override
    public void markFailed(String key) {
        keys.del(PREFIX + key);
    }

    private String toJson(IdempotentEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("幂等条目序列化失败", e);
        }
    }

    private IdempotentEntry fromJson(String json) {
        try {
            return objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), IdempotentEntry.class);
        } catch (Exception e) {
            throw new IllegalStateException("幂等条目反序列化失败", e);
        }
    }
}
