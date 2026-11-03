package io.github.foundry4j.quarkforge.redis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.github.foundry4j.quarkforge.ratelimit.RateLimiter;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

/// ## RedisRateLimiter
/// 基于 Redis 固定窗口计数器的分布式限流实现。
/// 普通 CDI Bean，优先级高于 ratelimit 模块中以 `@DefaultBean` 声明的内存实现，
/// 引入本模块后自动生效，无需额外配置。
@ApplicationScoped
public class RedisRateLimiter implements RateLimiter {

    private final ValueCommands<String, String> values;
    private final KeyCommands<String> keys;

    @Inject
    public RedisRateLimiter(RedisDataSource dataSource) {
        this.values = dataSource.value(String.class);
        this.keys = dataSource.key();
    }

    @Override
    public boolean tryAcquire(String key, int permits, long windowSeconds) {
        Long count = values.incr(key);
        if (count != null && count == 1L) {
            keys.expire(key, windowSeconds);
        }
        return count != null && count <= permits;
    }
}
