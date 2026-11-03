package io.github.foundry4j.quarkforge.web.file;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDownloadsTest {

    @Test
    void buildsRfc5987ContentDisposition() {
        String header = FileDownloads.contentDisposition("报告 2026.pdf", true);

        assertTrue(header.startsWith("attachment; filename=\""));
        assertTrue(header.contains("filename*=UTF-8''"));
        assertTrue(header.contains("%E6%8A%A5%E5%91%8A%202026.pdf"),
                "中文与空格应按 UTF-8 百分号编码: " + header);
    }

    @Test
    void fallsBackToAsciiFilename() {
        String header = FileDownloads.contentDisposition("report.pdf", false);

        assertEquals("inline; filename=\"report.pdf\"; filename*=UTF-8''report.pdf", header);
    }

    @Test
    void protectsAgainstHeaderInjection() {
        String header = FileDownloads.contentDisposition("a\"b\nc.txt", true);

        assertTrue(header.contains("filename=\"a_b_c.txt\""), header);
    }
}
