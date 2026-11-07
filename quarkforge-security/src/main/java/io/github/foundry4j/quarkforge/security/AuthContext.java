package io.github.foundry4j.quarkforge.security;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/// ## AuthContext
/// 当前请求认证上下文。在业务代码中直接注入使用：
///
/// ```java
/// @Inject AuthContext auth;
///
/// LoginUser user = auth.currentUser();   // 未登录时抛出 401 业务异常
/// ```
@ApplicationScoped
public class AuthContext {

    @Inject
    SecurityIdentity identity;

    /// 当前登录用户，未认证时抛出 `UNAUTHORIZED` 业务异常。
    public LoginUser currentUser() {
        if (identity.isAnonymous()) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
        }
        String username = identity.getPrincipal().getName();
        String id = username;
        Set<String> permissions = Set.of();
        if (identity.getPrincipal() instanceof JsonWebToken jwt) {
            if (jwt.getSubject() != null) {
                id = jwt.getSubject();
            }
            permissions = claimStrings(jwt, JwtTokenService.PERMISSIONS_CLAIM);
        }
        return new LoginUser(id, username, Set.copyOf(identity.getRoles()), permissions);
    }

    /// 是否已认证。
    public boolean isAuthenticated() {
        return !identity.isAnonymous();
    }

    /// 当前用户 ID，未认证时抛出 `UNAUTHORIZED`。
    public String userId() {
        return currentUser().id();
    }

    /// 当前用户是否拥有指定角色。
    public boolean hasRole(String role) {
        return identity.hasRole(role);
    }

    /// 当前用户是否拥有指定权限，未认证时返回 `false`。
    public boolean hasPermission(String permission) {
        if (identity.isAnonymous()) {
            return false;
        }
        return currentUser().hasPermission(permission);
    }

    /// 解析字符串数组类型的自定义 claim（SmallRye JWT 将 JSON 数组反序列化为 `List`）。
    private static Set<String> claimStrings(JsonWebToken jwt, String name) {
        Object claim = jwt.getClaim(name);
        if (claim instanceof Collection<?> values) {
            Set<String> result = new LinkedHashSet<>();
            values.forEach(value -> result.add(stripQuotes(String.valueOf(value))));
            return Set.copyOf(result);
        }
        if (claim instanceof String single && !single.isBlank()) {
            return Set.of(stripQuotes(single));
        }
        return Set.of();
    }

    /// SmallRye 对自定义数组 claim 可能返回 `JsonString`，其 `toString()` 带双引号。
    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
