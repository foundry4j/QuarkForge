package io.github.tuyucheng777.quarkforge.ratelimit;

/// ## KeyStrategy
/// 限流维度的取值策略。
public enum KeyStrategy {

    /// 按登录用户限流（未登录退化为客户端 IP）。
    USER,

    /// 按客户端 IP 限流。
    IP,

    /// 全局限流，所有调用方共享同一额度。
    GLOBAL
}
