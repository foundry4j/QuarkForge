package io.github.foundry4j.quarkforge.web.file;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class LocalFileStorageTest {

    @Inject
    FileStorage storage;

    @Test
    void storesReadsAndDeletesFile() throws Exception {
        String content = "hello quarkforge";

        StoredFile stored = storage.store(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                "notes.txt", "text/plain; charset=UTF-8");

        assertEquals("notes.txt", stored.originalFilename());
        assertEquals("text/plain", stored.contentType());
        assertEquals(content.length(), stored.size());
        assertTrue(stored.key().endsWith(".txt"), "key 应保留扩展名: " + stored.key());
        assertTrue(storage.exists(stored.key()));

        try (InputStream in = storage.open(stored.key())) {
            assertNotNull(in);
            assertEquals(content, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }

        storage.delete(stored.key());
        assertFalse(storage.exists(stored.key()));
    }

    @Test
    void rejectsFileLargerThanLimit() {
        byte[] big = new byte[5000];
        BizException error = assertThrows(BizException.class, () -> storage.store(
                new ByteArrayInputStream(big), "big.txt", "text/plain"));

        assertEquals(CommonErrorCode.FILE_TOO_LARGE.code(), error.code());
    }

    @Test
    void rejectsUnsupportedContentType() {
        BizException error = assertThrows(BizException.class, () -> storage.store(
                new ByteArrayInputStream(new byte[]{1, 2, 3}), "a.exe", "application/octet-stream"));

        assertEquals(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.code(), error.code());
    }

    @Test
    void rejectsPathTraversalKey() {
        BizException error = assertThrows(BizException.class,
                () -> storage.open("../../etc/passwd"));

        assertEquals(CommonErrorCode.PARAM_ERROR.code(), error.code());
    }
}
