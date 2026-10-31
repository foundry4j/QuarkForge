package io.github.tuyucheng777.quarkforge.idempotency;

/// ## IdempotentStore
/// 幂等状态存储抽象。
///
/// 默认实现为内存版（[InMemoryIdempotentStore]），引入 `quarkforge-redis` 后自动切换为
/// 分布式 Redis 实现。
public interface IdempotentStore {

    /// 尝试以 `PROCESSING` 状态占用幂等键。
    ///
    /// @param key        幂等键
    /// @param ttlSeconds 占用有效期（秒）
    /// @return 若键不存在或已过期则返回 `null`（表示当前请求是首次执行），
    ///         否则返回已有的条目（`PROCESSING` 或 `DONE`）
    IdempotentEntry tryAcquire(String key, long ttlSeconds);

    /// 业务方法执行成功，将结果写入存储并将状态置为 `DONE`。
    ///
    /// @param key        幂等键
    /// @param payload    返回值的序列化字节
    /// @param ttlSeconds 结果缓存时长（秒）
    void markDone(String key, byte[] payload, long ttlSeconds);

    /// 业务方法执行失败，删除幂等键以允许后续重试。
    ///
    /// @param key 幂等键
    void markFailed(String key);
}
