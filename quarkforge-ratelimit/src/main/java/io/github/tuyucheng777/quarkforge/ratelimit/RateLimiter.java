package io.github.tuyucheng777.quarkforge.ratelimit;

/// ## RateLimiter
/// 限流后端 SPI。默认实现为基于内存固定窗口的 [InMemoryRateLimiter]；
/// 引入 `quarkforge-redis` 模块后自动切换为 Redis 分布式限流实现。
public interface RateLimiter {

    /// 尝试获取一次调用许可。
    ///
    /// @param key           限流键（已含方法与维度信息）
    /// @param permits       窗口内允许的请求数
    /// @param windowSeconds 窗口长度（秒）
    /// @return `true` 表示放行，`false` 表示超出额度
    boolean tryAcquire(String key, int permits, long windowSeconds);
}
