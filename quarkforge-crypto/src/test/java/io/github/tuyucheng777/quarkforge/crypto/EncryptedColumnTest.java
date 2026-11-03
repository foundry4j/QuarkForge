package io.github.tuyucheng777.quarkforge.crypto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class EncryptedColumnTest {

    @Inject
    Fixture fixture;

    @Test
    void storesCiphertextAndReturnsPlaintext() {
        String plaintext = "13800000000";
        Long id = fixture.create(plaintext);

        String stored = fixture.rawPhone(id);
        assertTrue(stored.startsWith("v1:"), "数据库应存储密文");
        assertNotEquals(plaintext, stored);

        assertEquals(plaintext, fixture.phone(id));
    }

    @ApplicationScoped
    public static class Fixture {

        @Transactional
        public Long create(String phone) {
            SecretUser user = new SecretUser();
            user.phone = phone;
            user.persist();
            return user.id;
        }

        @Transactional
        public String rawPhone(Long id) {
            return (String) SecretUser.getEntityManager()
                    .createNativeQuery("select phone from qf_test_secret_user where id = ?1", String.class)
                    .setParameter(1, id)
                    .getSingleResult();
        }

        @Transactional
        public String phone(Long id) {
            SecretUser user = SecretUser.findById(id);
            return user == null ? null : user.phone;
        }
    }
}
