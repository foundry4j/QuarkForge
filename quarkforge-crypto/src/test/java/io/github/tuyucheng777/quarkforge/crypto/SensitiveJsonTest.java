package io.github.tuyucheng777.quarkforge.crypto;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SensitiveJsonTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    void masksSensitiveFieldsInJson() throws Exception {
        UserView view = new UserView();
        view.phone = "13800000000";
        view.email = "alice@example.com";
        view.name = "张三";
        view.password = "p@ssw0rd";

        String json = objectMapper.writeValueAsString(view);

        assertTrue(json.contains("138****0000"));
        assertTrue(json.contains("a***@example.com"));
        assertTrue(json.contains("张*"));
        assertFalse(json.contains("p@ssw0rd"));
    }

    static class UserView {
        @Sensitive(SensitiveType.PHONE)
        public String phone;

        @Sensitive(SensitiveType.EMAIL)
        public String email;

        @Sensitive(SensitiveType.NAME)
        public String name;

        @Sensitive(SensitiveType.PASSWORD)
        public String password;
    }
}
