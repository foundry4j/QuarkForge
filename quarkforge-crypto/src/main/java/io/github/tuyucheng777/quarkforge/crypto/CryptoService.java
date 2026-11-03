package io.github.tuyucheng777.quarkforge.crypto;

/// ## CryptoService
/// 字段加密 SPI。默认实现 [AesGcmCryptoService] 使用 AES-256-GCM；
/// 如需接入 KMS、国密等方案，提供同类型 Bean 即可覆盖默认实现。
///
/// 加密结果带版本前缀（默认 `v1:`），便于未来平滑轮换密钥或算法。
public interface CryptoService {

    /// 加密明文，`null` 原样返回。
    String encrypt(String plaintext);

    /// 解密密文，`null` 原样返回；格式非法时抛出 [IllegalArgumentException]。
    String decrypt(String ciphertext);
}
