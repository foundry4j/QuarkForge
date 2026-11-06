package io.github.foundry4j.quarkforge.test;

import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/// ## TestAuthTest
/// 验证身份模拟的打桩与 `AuthContext` 的解析口径一致。
class TestAuthTest {

    @Test
    void authenticateStubsFullIdentity() {
        SecurityIdentity identity = mock(SecurityIdentity.class);

        TestAuth.authenticate(identity, "u1", "tom", Set.of("admin"), List.of("user:create"));

        assertFalse(identity.isAnonymous());
        assertEquals(Set.of("admin"), identity.getRoles());
        assertEquals("tom", identity.getPrincipal().getName());

        JsonWebToken jwt = assertInstanceOf(JsonWebToken.class, identity.getPrincipal());
        assertEquals("u1", jwt.getSubject());
        assertEquals(List.of("user:create"), jwt.getClaim(TestAuth.PERMISSIONS_CLAIM));
    }

    @Test
    void authenticateUsesDefaultIdentity() {
        SecurityIdentity identity = mock(SecurityIdentity.class);

        TestAuth.authenticate(identity, Set.of("user"));

        assertEquals("tester", identity.getPrincipal().getName());
        assertEquals("test-user", ((JsonWebToken) identity.getPrincipal()).getSubject());
    }

    @Test
    void anonymousStubsAnonymousIdentity() {
        SecurityIdentity identity = mock(SecurityIdentity.class);

        TestAuth.anonymous(identity);

        assertTrue(identity.isAnonymous());
    }
}
