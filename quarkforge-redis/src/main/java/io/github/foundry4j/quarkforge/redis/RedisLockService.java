package io.github.foundry4j.quarkforge.redis;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.vertx.mutiny.redis.client.Response;

/// ## RedisLockService
/// 基于 Redis 的分布式锁：
///
/// - 加锁：`SET NX EX`，返回的令牌用于安全解锁
/// - 解锁：Lua 脚本校验令牌后删除，避免误删他人持有的锁
///
/// ```java
/// String token = lockService.tryLock("order:pay:" + orderId, Duration.ofSeconds(30));
/// if (token == null) {
///     throw new BizException(CommonErrorCode.SERVICE_UNAVAILABLE, "操作进行中，请勿重复提交");
/// }
/// try {
///     ...
/// } finally {
///     lockService.unlock("order:pay:" + orderId, token);
/// }
/// ```
@ApplicationScoped
public class RedisLockService {

    private static final String UNLOCK_SCRIPT = """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else
                return 0
            end
            """;

    private final ValueCommands<String, String> values;
    private final KeyCommands<String> keys;
    private final RedisDataSource dataSource;

    @Inject
    public RedisLockService(RedisDataSource dataSource) {
        this.dataSource = dataSource;
        this.values = dataSource.value(String.class);
        this.keys = dataSource.key();
    }

    /// 尝试加锁。成功返回解锁令牌；锁已被占用时返回 `null`，不会阻塞等待。
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = values.setnx(key, token);
        if (Boolean.TRUE.equals(acquired)) {
            keys.expire(key, ttl.toSeconds());
            return token;
        }
        return null;
    }

    /// 安全解锁：仅当锁仍由 `token` 持有时才删除。
    public boolean unlock(String key, String token) {
        Response response = dataSource.execute("EVAL", UNLOCK_SCRIPT, "1", key, token);
        return response != null && response.toLong() == 1L;
    }

    /// 在锁保护下执行任务，自动处理加锁/解锁；抢锁失败时抛出 [LockedException]。
    public <T> T withLock(String key, Duration ttl, Supplier<T> action) {
        String token = tryLock(key, ttl);
        if (token == null) {
            throw new LockedException("获取分布式锁失败: " + key);
        }
        try {
            return action.get();
        } finally {
            unlock(key, token);
        }
    }

    /// 在锁保护下执行任务（无返回值）。
    public void withLock(String key, Duration ttl, Runnable action) {
        withLock(key, ttl, () -> {
            action.run();
            return null;
        });
    }

    /// 抢锁失败时抛出。
    public static class LockedException extends RuntimeException {
        public LockedException(String message) {
            super(message);
        }
    }
}
