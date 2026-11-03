package io.github.foundry4j.quarkforge.cache;

import java.time.Duration;
import java.util.function.Supplier;

/// ## CacheService
/// 通用缓存 SPI。
///
/// - 引入 `quarkforge-cache`：默认使用 Caffeine 本地缓存（[LocalCacheService]）
/// - 再引入 `quarkforge-redis`：自动切换为本地 + Redis 两级缓存
///
/// 键不存在、已过期或缓存中不是目标类型时返回 `null`；`loader` 返回 `null` 时不写入缓存。
public interface CacheService {

    /// 读取缓存。`ttl` 仅用于需要回填本地层的实现（如两级缓存），本地缓存实现会忽略它。
    <T> T get(String key, Class<T> type, Duration ttl);

    /// 写入缓存并设置过期时间；`ttl` 为 `null`、零或负数时使用默认 TTL。
    <T> void put(String key, T value, Duration ttl);

    /// 旁路读取：未命中时调用 `loader` 加载并回写。
    <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader);

    /// 删除单个键。
    void evict(String key);
}
