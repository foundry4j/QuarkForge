package io.github.tuyucheng777.quarkforge.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.DefaultBean;

/// ## AesGcmCryptoService
/// 默认字段加密实现：AES/GCM/NoPadding，每次加密使用随机 12 字节 IV。
///
/// 密文格式：`v1:` + Base64(IV ‖ 密文 ‖ 认证标签)，可直接存入 `VARCHAR` 字段。
///
/// ```properties
/// # 32 字节密钥的 Base64 编码，可用 openssl rand -base64 32 生成
/// quarkforge.crypto.key=...
/// ```
///
/// 配置缺失或长度非法时启动失败，避免使用随机密钥导致重启后无法解密。
@DefaultBean
@ApplicationScoped
public class AesGcmCryptoService implements CryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String TRANSFORMATION_VERSION = "v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom random = new SecureRandom();

    @Inject
    @ConfigProperty(name = "quarkforge.crypto.key", defaultValue = "")
    String configuredKey;

    private SecretKey key;

    @PostConstruct
    void init() {
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException(
                    "未配置字段加密密钥，请设置 quarkforge.crypto.key（32 字节密钥的 Base64）");
        }
        byte[] raw = Base64.getDecoder().decode(configuredKey.trim());
        if (raw.length != 16 && raw.length != 24 && raw.length != 32) {
            throw new IllegalStateException(
                    "quarkforge.crypto.key 长度非法，必须为 16/24/32 字节，当前为 " + raw.length + " 字节");
        }
        key = new SecretKeySpec(raw, "AES");
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return TRANSFORMATION_VERSION + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("字段加密失败", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        if (!ciphertext.startsWith(TRANSFORMATION_VERSION)) {
            throw new IllegalArgumentException("不支持的密文格式，期望以 " + TRANSFORMATION_VERSION + " 开头");
        }
        byte[] payload = Base64.getDecoder().decode(ciphertext.substring(TRANSFORMATION_VERSION.length()));
        if (payload.length <= IV_LENGTH) {
            throw new IllegalArgumentException("密文长度非法");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] decrypted = cipher.doFinal(payload, IV_LENGTH, payload.length - IV_LENGTH);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("字段解密失败，密钥不匹配或密文被篡改", e);
        }
    }
}
