package io.github.tuyucheng777.quarkforge.idempotency;

/// ## IdempotentEntry
/// 幂等键在存储中的状态。
///
/// @param state     当前状态
/// @param payload   业务方法返回值的序列化字节（`DONE` 时有效，`PROCESSING` 时为 `null`）
/// @param expiresAt 过期时间戳（毫秒），惰性判定过期
public record IdempotentEntry(State state, byte[] payload, long expiresAt) {

    /// 幂等键状态。
    public enum State {
        /// 业务方法正在执行，尚未返回结果。
        PROCESSING,
        /// 业务方法已执行完成，`payload` 中保存了可回放的结果。
        DONE
    }
}
