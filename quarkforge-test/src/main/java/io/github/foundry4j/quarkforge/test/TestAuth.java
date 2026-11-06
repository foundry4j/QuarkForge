package io.github.foundry4j.quarkforge.test;

import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// ## TestAuth
/// 配合 `@InjectMock SecurityIdentity` 模拟登录身份的测试工具：
///
/// ```java
/// @QuarkusTest
/// class OrderServiceTest {
///
///     @InjectMock
///     SecurityIdentity identity;
///
///     @Test
///     void adminCanCloseOrder() {
///         TestAuth.authenticate(identity, Set.of("admin"));
///         // ...
///     }
///
///     @Test
///     void anonymousGets401() {
///         TestAuth.anonymous(identity);
///         // ...
///     }
/// }
/// ```
///
/// 与 `quarkforge-security` 的 `AuthContext` 解析逻辑对齐：
/// 用户 ID 走 `sub` claim、用户名走 `upn`、权限集合走 `perms` claim。
public final class TestAuth {

    /// JWT 权限 claim 名（与 `JwtTokenService.PERMISSIONS_CLAIM` 一致；
    /// 硬编码以避免测试模块反向依赖安全模块）。
    public static final String PERMISSIONS_CLAIM = "perms";

    private TestAuth() {
    }

    /// 模拟一个已登录用户，使用默认用户 ID / 用户名。
    public static void authenticate(SecurityIdentity identity, Set<String> roles) {
        authenticate(identity, "test-user", "tester", roles, List.of());
    }

    /// 模拟一个已登录用户，完整指定身份与权限。
    public static void authenticate(SecurityIdentity identity, String userId, String username,
                                    Set<String> roles, List<String> permissions) {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getName()).thenReturn(username);
        when(jwt.getSubject()).thenReturn(userId);
        doReturn(permissions).when(jwt).getClaim(PERMISSIONS_CLAIM);
        when(identity.isAnonymous()).thenReturn(false);
        when(identity.getPrincipal()).thenReturn(jwt);
        when(identity.getRoles()).thenReturn(roles);
    }

    /// 模拟匿名（未登录）访问。
    public static void anonymous(SecurityIdentity identity) {
        when(identity.isAnonymous()).thenReturn(true);
    }
}
