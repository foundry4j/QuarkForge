package io.github.foundry4j.quarkforge.web.file;

/// 已存储文件的元信息。业务侧通常把 [key] 与元信息一起落库，
/// 下载时按 key 取回内容。
///
/// @param key              存储键，由 [FileStorage] 生成，不可由调用方指定
/// @param originalFilename 用户上传时的原始文件名
/// @param contentType      规范化后的 MIME 类型（已去掉 `;charset=` 等参数）
/// @param size             实际字节数
public record StoredFile(String key, String originalFilename, String contentType, long size) {
}
