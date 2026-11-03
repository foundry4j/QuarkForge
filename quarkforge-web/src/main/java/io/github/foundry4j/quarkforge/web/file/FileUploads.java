package io.github.foundry4j.quarkforge.web.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.jboss.resteasy.reactive.multipart.FileUpload;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;

/// multipart 上传辅助工具，把 RESTEasy Reactive 的 [FileUpload]
/// 直接交给 [FileStorage] 落盘。
///
/// ```java
/// @POST
/// @Consumes(MediaType.MULTIPART_FORM_DATA)
/// public StoredFile upload(@RestForm FileUpload file) throws IOException {
///     return FileUploads.store(storage, file);
/// }
/// ```
public final class FileUploads {

    private FileUploads() {
    }

    /// 读取上传内容并交给存储 SPI。
    public static StoredFile store(FileStorage storage, FileUpload upload) {
        if (upload == null) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "缺少上传文件");
        }
        try (InputStream in = Files.newInputStream(upload.uploadedFile())) {
            return storage.store(in, upload.fileName(), upload.contentType());
        } catch (IOException e) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "读取上传文件失败", e);
        }
    }
}
