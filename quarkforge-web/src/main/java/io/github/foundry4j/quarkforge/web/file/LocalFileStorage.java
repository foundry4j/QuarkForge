package io.github.foundry4j.quarkforge.web.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.configuration.MemorySize;

/// 本地磁盘文件存储，默认实现。
///
/// 存储布局按日期分目录，避免单目录文件过多：
///
/// ```text
/// {base-dir}/2026/11/03/8f3a....pdf
/// ```
///
/// 配置项：
///
/// | 配置 | 默认值 | 说明 |
/// |------|--------|------|
/// | `quarkforge.web.file.base-dir` | `${java.io.tmpdir}/quarkforge-files` | 存储根目录 |
/// | `quarkforge.web.file.max-size` | `10M` | 单文件大小上限，超出返回 413 |
/// | `quarkforge.web.file.allowed-content-types` | 空（不限制） | 允许的 MIME 类型，逗号分隔 |
@DefaultBean
@ApplicationScoped
public class LocalFileStorage implements FileStorage {

    private static final DateTimeFormatter DATE_DIRS = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final int BUFFER_SIZE = 8192;

    @ConfigProperty(name = "quarkforge.web.file.base-dir", defaultValue = "${java.io.tmpdir}/quarkforge-files")
    String baseDir;

    @ConfigProperty(name = "quarkforge.web.file.max-size", defaultValue = "10M")
    MemorySize maxSize;

    @ConfigProperty(name = "quarkforge.web.file.allowed-content-types")
    Optional<List<String>> allowedContentTypes;

    private Path root;

    @PostConstruct
    void init() throws IOException {
        root = Paths.get(baseDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public StoredFile store(InputStream in, String originalFilename, String contentType) {
        String normalizedType = normalizeContentType(contentType);
        assertContentTypeAllowed(normalizedType);

        long limit = maxSize.asBigInteger().longValueExact();
        String key = buildKey(originalFilename);
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            throw new BizException(CommonErrorCode.INTERNAL_ERROR, "无法创建存储目录", e);
        }

        long total = 0;
        boolean success = false;
        try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > limit) {
                    throw new BizException(CommonErrorCode.FILE_TOO_LARGE,
                            "文件大小超过上限 " + maxSize);
                }
                out.write(buffer, 0, read);
            }
            success = true;
        } catch (IOException e) {
            throw new BizException(CommonErrorCode.INTERNAL_ERROR, "文件写入失败", e);
        } finally {
            if (!success) {
                deleteQuietly(target);
            }
        }
        return new StoredFile(key, sanitizeFilename(originalFilename), normalizedType, total);
    }

    @Override
    public InputStream open(String key) throws IOException {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        return Files.newInputStream(path);
    }

    @Override
    public boolean exists(String key) {
        return Files.isRegularFile(resolve(key));
    }

    @Override
    public void delete(String key) {
        deleteQuietly(resolve(key));
    }

    /// 解析存储键并做路径穿越防护：任何越出根目录的 key 都视为非法参数。
    private Path resolve(String key) {
        if (key == null || key.isBlank()) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "文件 key 不能为空");
        }
        Path path = root.resolve(key).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "非法的文件 key");
        }
        return path;
    }

    /// 生成 `yyyy/MM/dd/<uuid><ext>` 形式的存储键。
    private String buildKey(String originalFilename) {
        String extension = extensionOf(originalFilename);
        return LocalDate.now().format(DATE_DIRS) + "/"
                + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    /// 只保留最后一个点之后、由字母数字组成的扩展名（最长 10 位）。
    private String extensionOf(String filename) {
        String safe = sanitizeFilename(filename);
        int dot = safe.lastIndexOf('.');
        if (dot <= 0 || dot == safe.length() - 1) {
            return "";
        }
        String ext = safe.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return ext.isEmpty() || ext.length() > 10 ? "" : "." + ext;
    }

    /// 去掉路径部分与控制字符，仅用于回显元信息。
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        String name = filename.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        String cleaned = name.replaceAll("[\\p{Cntrl}]", "").trim();
        return cleaned.isEmpty() ? "file" : cleaned;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private void assertContentTypeAllowed(String contentType) {
        allowedContentTypes.ifPresent(types -> {
            List<String> allowed = types.stream()
                    .map(type -> type.trim().toLowerCase(Locale.ROOT))
                    .toList();
            if (!allowed.isEmpty() && !allowed.contains(contentType)) {
                throw new BizException(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE,
                        "不支持的文件类型: " + contentType);
            }
        });
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 清理失败不影响主流程，后续可人工清理
        }
    }
}
