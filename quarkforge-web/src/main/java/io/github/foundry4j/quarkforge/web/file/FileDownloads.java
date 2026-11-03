package io.github.foundry4j.quarkforge.web.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;

/// 文件下载辅助工具：流式输出，避免把大文件整体读入内存；并按 RFC 5987
/// 生成 `Content-Disposition`，中文文件名在浏览器中可正确显示。
///
/// ```java
/// @GET
/// @Path("/{id}/content")
/// public Response download(@RestPath Long id) {
///     FileRecord record = repository.requireById(id);
///     return FileDownloads.download(storage, record.key(), record.filename(), record.contentType());
/// }
/// ```
public final class FileDownloads {

    private FileDownloads() {
    }

    /// 以附件形式下载文件。文件不存在时抛出 404 业务异常。
    public static Response download(FileStorage storage, String key, String filename, String contentType) {
        return build(storage, key, filename, contentType, true);
    }

    /// 以内联形式返回文件（浏览器可预览的图片/PDF 等）。
    public static Response inline(FileStorage storage, String key, String filename, String contentType) {
        return build(storage, key, filename, contentType, false);
    }

    private static Response build(FileStorage storage, String key, String filename,
                                  String contentType, boolean attachment) {
        InputStream in;
        try {
            in = storage.open(key);
        } catch (IOException e) {
            throw new BizException(CommonErrorCode.INTERNAL_ERROR, "读取文件失败", e);
        }
        if (in == null) {
            throw new BizException(CommonErrorCode.NOT_FOUND, "文件不存在");
        }
        return Response.ok(in)
                .type(contentType == null || contentType.isBlank()
                        ? MediaType.APPLICATION_OCTET_STREAM : contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename, attachment))
                .build();
    }

    /// 生成兼容 RFC 5987 的 `Content-Disposition`：
    /// `attachment; filename="ascii-fallback"; filename*=UTF-8''percent-encoded`。
    public static String contentDisposition(String filename, boolean attachment) {
        String name = filename == null || filename.isBlank() ? "download" : filename;
        return (attachment ? "attachment" : "inline")
                + "; filename=\"" + asciiFallback(name) + "\""
                + "; filename*=UTF-8''" + encode(name);
    }

    /// 非 ASCII 字符回退为下划线，供不支持 `filename*` 的老客户端使用。
    private static String asciiFallback(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (char c : name.toCharArray()) {
            sb.append(c < 0x20 || c > 0x7E || c == '"' ? '_' : c);
        }
        return sb.toString();
    }

    /// 按 RFC 3986 unreserved 集合做百分号编码（空格编码为 `%20`）。
    private static String encode(String value) {
        StringBuilder sb = new StringBuilder(value.length() * 3);
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if (isUnreserved(c)) {
                sb.append(c);
            } else {
                sb.append('%').append(String.format("%02X", b & 0xFF));
            }
        }
        return sb.toString();
    }

    private static boolean isUnreserved(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9')
                || c == '-' || c == '.' || c == '_' || c == '~';
    }
}
