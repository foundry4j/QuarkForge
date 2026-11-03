package io.github.foundry4j.quarkforge.outbox;

/// ## OutboxStatus
/// 发件箱消息状态流转：
///
/// `PENDING` → `SENDING` → `SENT`；投递失败则回到 `PENDING` 等待重试，
/// 超过最大重试次数后进入 `FAILED`。
public enum OutboxStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED
}
