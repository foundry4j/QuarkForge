package io.github.foundry4j.quarkforge.core.util;

/// ## SnowflakeIdGenerator
/// 纯 JDK 实现的雪花 ID 生成器（64 位 long）：
///
/// ```
/// 0 | 41 位时间戳（毫秒，相对自定义纪元） | 10 位节点 | 12 位序列
/// ```
///
/// - 每节点每毫秒最多 4096 个 ID
/// - 节点 ID 范围 [0, 1023]，多实例部署时需保证唯一
///
/// 非线程安全之外的额外依赖：无。实例线程安全。
public final class SnowflakeIdGenerator {

    /// 自定义纪元：2025-01-01 00:00:00 UTC，可使用约 69 年。
    public static final long DEFAULT_EPOCH = 1735689600000L;

    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;
    private final long epoch;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId) {
        this(workerId, DEFAULT_EPOCH);
    }

    public SnowflakeIdGenerator(long workerId, long epoch) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId 必须在 [0, " + MAX_WORKER_ID + "] 范围内: " + workerId);
        }
        this.workerId = workerId;
        this.epoch = epoch;
    }

    /// 生成下一个全局唯一 ID。
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("检测到时钟回拨，拒绝生成 ID");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - epoch) << TIMESTAMP_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
    }

    private long waitNextMillis(long last) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= last) {
            Thread.onSpinWait();
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
