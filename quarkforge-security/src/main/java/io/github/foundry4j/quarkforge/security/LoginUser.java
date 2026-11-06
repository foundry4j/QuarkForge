package io.github.foundry4j.quarkforge.security;

import java.io.Serializable;
import java.util.Set;

/// ## LoginUser
/// 当前登录用户视图，从 JWT claims 中解析而来。
///
/// @param id          用户 ID（`sub` claim）
/// @param username    用户名（`upn` claim）
/// @param roles       角色集合（`groups` claim）
/// @param permissions 权限集合（`perms` claim，签发时由业务方按角色展开写入）
public record LoginUser(String id, String username, Set<String> roles, Set<String> permissions)
        implements Serializable {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
