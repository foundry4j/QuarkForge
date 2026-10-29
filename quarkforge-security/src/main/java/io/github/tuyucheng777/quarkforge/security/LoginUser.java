package io.github.tuyucheng777.quarkforge.security;

import java.io.Serializable;
import java.util.Set;

/// ## LoginUser
/// 当前登录用户视图，从 JWT claims 中解析而来。
///
/// @param id       用户 ID（`sub` claim）
/// @param username 用户名（`upn` claim）
/// @param roles    角色集合（`groups` claim）
public record LoginUser(String id, String username, Set<String> roles) implements Serializable {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
