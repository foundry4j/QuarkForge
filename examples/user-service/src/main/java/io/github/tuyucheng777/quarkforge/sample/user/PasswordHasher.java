package io.github.tuyucheng777.quarkforge.sample.user;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/// ## PasswordHasher
/// 基于 JDK 内置 PBKDF2WithHmacSHA256 的密码哈希工具，仅依赖 JDK，无额外加密库。
final class PasswordHasher {

    private static final int ITERATIONS = 10_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /// 生成随机盐值（Base64）。
    static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /// 计算密码哈希（十六进制），盐值需与 [newSalt] 产出的值一致。
    static String hash(String rawPassword, String salt) {
        PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(),
                Base64.getDecoder().decode(salt), ITERATIONS, KEY_LENGTH_BITS);
        try {
            byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(derived);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("密码哈希失败", e);
        } finally {
            spec.clearPassword();
        }
    }

    /// 校验密码是否与既有哈希匹配。
    static boolean matches(String rawPassword, String salt, String expectedHash) {
        return java.security.MessageDigest.isEqual(
                hash(rawPassword, salt).getBytes(), expectedHash.getBytes());
    }
}
