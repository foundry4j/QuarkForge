package io.github.foundry4j.quarkforge.security;

/// ## Logical
/// 多个必需角色 / 权限之间的组合方式。
public enum Logical {

    /// 满足任意一个即放行。
    ANY,

    /// 必须全部满足才放行。
    ALL
}
