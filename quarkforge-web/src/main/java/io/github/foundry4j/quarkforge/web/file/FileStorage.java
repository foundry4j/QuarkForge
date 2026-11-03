package io.github.foundry4j.quarkforge.web.file;

import java.io.IOException;
import java.io.InputStream;

/// 文件存储 SPI。默认实现是本地磁盘 [LocalFileStorage]；业务也可以提供
/// 自己的实现（例如对象存储 OSS/S3/MinIO），只要注册一个普通 Bean 即可
/// 覆盖默认实现。
///
/// ```java
/// @ApplicationScoped
/// public class OssFileStorage implements FileStorage { ... }
/// ```
public interface FileStorage {

    /// 保存文件内容，返回存储元信息。
    ///
    /// 实现需保证：文件名只用于推导扩展名，绝不拼接进路径（避免路径穿越）；
    /// 超出大小限制或不支持的 MIME 类型时抛出 `BizException`。
    ///
    /// @param in               文件内容流，调用方负责关闭
    /// @param originalFilename 原始文件名
    /// @param contentType      MIME 类型，可为 `null`
    StoredFile store(InputStream in, String originalFilename, String contentType);

    /// 按 key 打开文件内容；不存在时返回 `null`。
    InputStream open(String key) throws IOException;

    /// 文件是否存在。
    boolean exists(String key);

    /// 删除文件；不存在时静默返回。
    void delete(String key);
}
