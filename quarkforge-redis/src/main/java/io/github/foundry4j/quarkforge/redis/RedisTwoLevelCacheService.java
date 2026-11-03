package io.github.foundry4j.quarkforge.redis;

import java.time.Duration;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.github.foundry4j.quarkforge.cache.CacheService;
import io.github.foundry4j.quarkforge.cache.LocalCacheService;

/// ## RedisTwoLevelCacheService
/// 两级缓存：先查 Caffeine 本地缓存，未命中再查 Redis；Redis 命中后回填本地。
///
/// 引入 `quarkforge-redis` 后自动覆盖 `quarkforge-cache` 提供的
/// [LocalCacheService]，业务代码只需注入 [CacheService]：
///
/// ```java
/// User user = cache.getOrLoad("user:" + id, User.class, Duration.ofMinutes(10),
///         () -> userRepository.requireById(id));
/// ```
///
/// - 读：本地 → Redis → `loader`
/// - 写：同时写本地与 Redis
/// - 删除：同时删除本地与 Redis，避免其他节点读到本地旧值
///
/// 注意：多节点部署时本地缓存无法感知其他节点的删除操作，请为本地层设置较短 TTL。
@ApplicationScoped
public class RedisTwoLevelCacheService implements CacheService {

    private final LocalCacheService local;
    private final RedisCacheService redis;

    @Inject
    public RedisTwoLevelCacheService(LocalCacheService local, RedisCacheService redis) {
        this.local = local;
        this.redis = redis;
    }

    @Override
    public <T> T get(String key, Class<T> type, Duration ttl) {
        T localValue = local.get(key, type, ttl);
        if (localValue != null) {
            return localValue;
        }
        T remoteValue = redis.get(key, type);
        if (remoteValue != null) {
            local.put(key, remoteValue, ttl);
        }
        return remoteValue;
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        if (key == null || value == null) {
            return;
        }
        local.put(key, value, ttl);
        redis.set(key, value, ttl);
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        T cached = get(key, type, ttl);
        if (cached != null) {
            return cached;
        }
        T loaded = loader.get();
        if (loaded != null) {
            put(key, loaded, ttl);
        }
        return loaded;
    }

    @Override
    public void evict(String key) {
        local.evict(key);
        redis.evict(key);
    }
}
