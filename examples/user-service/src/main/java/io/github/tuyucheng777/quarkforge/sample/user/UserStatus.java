package io.github.tuyucheng777.quarkforge.sample.user;

/// ## UserStatus
/// 用户状态，持久化为字符串，便于后续扩展。
public enum UserStatus {

    /// 正常可用。
    ACTIVE,

    /// 已禁用，禁止登录。
    DISABLED
}
