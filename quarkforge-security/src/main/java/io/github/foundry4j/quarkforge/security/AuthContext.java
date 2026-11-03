package io.github.foundry4j.quarkforge.security;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.quarkus.security.identity.SecurityIdentity;

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
        if (identity.getPrincipal() instanceof JsonWebToken jwt && jwt.getSubject() != null) {
            id = jwt.getSubject();
        }
        return new LoginUser(id, username, Set.copyOf(identity.getRoles()));
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
}
