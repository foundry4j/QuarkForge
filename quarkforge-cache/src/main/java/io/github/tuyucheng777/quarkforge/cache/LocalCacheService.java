package io.github.tuyucheng777.quarkforge.cache;

import java.time.Duration;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

import io.quarkus.arc.DefaultBean;

/// ## LocalCacheService
/// Caffeine 本地缓存实现，单机零依赖，默认存活 5 分钟、最多 10000 条。
///
/// ```properties
/// quarkforge.cache.local.max-size=10000
/// quarkforge.cache.local.default-ttl=PT5M
/// ```
///
/// 引入 `quarkforge-redis` 后会被 [io.github.tuyucheng777.quarkforge.redis.RedisTwoLevelCacheService]
/// 覆盖，业务代码仍然只依赖 [CacheService]。
@DefaultBean
@ApplicationScoped
public class LocalCacheService implements CacheService {

    @Inject
    @ConfigProperty(name = "quarkforge.cache.local.max-size", defaultValue = "10000")
    long maxSize;

    @Inject
    @ConfigProperty(name = "quarkforge.cache.local.default-ttl", defaultValue = "PT5M")
    Duration defaultTtl;

    private Cache<String, CachedValue> cache;

    @PostConstruct
    void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfter(new Expiry<String, CachedValue>() {
                    @Override
                    public long expireAfterCreate(String key, CachedValue value, long currentTime) {
                        return value.ttlNanos();
                    }

                    @Override
                    public long expireAfterUpdate(String key, CachedValue value, long currentTime,
                                                  long currentDuration) {
                        return value.ttlNanos();
                    }

                    @Override
                    public long expireAfterRead(String key, CachedValue value, long currentTime,
                                                long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    @Override
    public <T> T get(String key, Class<T> type, Duration ttl) {
        CachedValue cached = cache.getIfPresent(key);
        return cached == null ? null : type.cast(cached.value());
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        if (key == null || value == null) {
            return;
        }
        cache.put(key, new CachedValue(value, ttlNanos(ttl)));
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
        cache.invalidate(key);
    }

    /// 当前缓存条目数（会先清理过期条目）。
    public long estimatedSize() {
        cache.cleanUp();
        return cache.estimatedSize();
    }

    private long ttlNanos(Duration ttl) {
        Duration effective = ttl == null || ttl.isZero() || ttl.isNegative() ? defaultTtl : ttl;
        return Math.max(effective.toNanos(), 0);
    }

    private record CachedValue(Object value, long ttlNanos) {
    }
}
