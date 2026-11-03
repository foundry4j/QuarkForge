package io.github.foundry4j.quarkforge.core.util;

import org.junit.jupiter.api.Test;
import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChecksTest {

    @Test
    void notBlankPassesForNormalString() {
        assertDoesNotThrow(() -> Checks.notBlank("abc", "name"));
    }

    @Test
    void notBlankRejectsBlankString() {
        BizException ex = assertThrows(BizException.class, () -> Checks.notBlank("  ", "name"));

        assertEquals(CommonErrorCode.PARAM_ERROR.code(), ex.code());
    }

    @Test
    void isTrueThrowsWithGivenErrorCode() {
        BizException ex = assertThrows(BizException.class,
                () -> Checks.isTrue(false, CommonErrorCode.FORBIDDEN, "不可操作"));

        assertEquals(40300, ex.code());
        assertEquals("不可操作", ex.getMessage());
    }
}
