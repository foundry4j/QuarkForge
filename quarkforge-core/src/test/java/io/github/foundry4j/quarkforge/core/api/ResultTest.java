package io.github.foundry4j.quarkforge.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @Test
    void okCarriesDataAndSuccessCode() {
        Result<String> result = Result.ok("hello");

        assertTrue(result.success());
        assertEquals(200, result.code());
        assertEquals("hello", result.data());
        assertTrue(result.timestamp() > 0);
    }

    @Test
    void errorUsesErrorCodeDefaults() {
        Result<Void> result = Result.error(CommonErrorCode.NOT_FOUND);

        assertFalse(result.success());
        assertEquals(40400, result.code());
        assertEquals(CommonErrorCode.NOT_FOUND.message(), result.message());
        assertNull(result.data());
    }

    @Test
    void errorAllowsCustomMessage() {
        Result<Void> result = Result.error(CommonErrorCode.PARAM_ERROR, "id 非法");

        assertEquals(40000, result.code());
        assertEquals("id 非法", result.message());
    }
}
