package io.github.foundry4j.quarkforge.crypto;

/// ## Masking
/// 脱敏规则实现，供 [SensitiveSerializer] 与业务代码直接调用。
public final class Masking {

    private static final String PASSWORD_MASK = "******";

    private Masking() {
    }

    /// 按类型脱敏，`null` 原样返回。
    public static String mask(String value, SensitiveType type) {
        if (value == null) {
            return null;
        }
        return switch (type == null ? SensitiveType.DEFAULT : type) {
            case PHONE -> keepEnds(value, 3, 4, 4);
            case ID_CARD -> keepEnds(value, 6, 4, 4);
            case EMAIL -> maskEmail(value);
            case BANK_CARD -> maskBankCard(value);
            case NAME -> keepEnds(value, 1, 0, 1);
            case PASSWORD -> PASSWORD_MASK;
            case DEFAULT -> keepEnds(value, 1, 1, 1);
        };
    }

    /// 保留开头 [prefix] 位与结尾 [suffix] 位，中间用 `*` 填充（至少 [minStars] 个）。
    private static String keepEnds(String value, int prefix, int suffix, int minStars) {
        int length = value.length();
        if (length <= prefix + suffix) {
            return value.substring(0, Math.min(prefix, length)) + "*".repeat(Math.max(length - prefix, 1));
        }
        int masked = Math.max(length - prefix - suffix, minStars);
        return value.substring(0, prefix) + "*".repeat(masked) + value.substring(length - suffix);
    }

    private static String maskEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 0) {
            return keepEnds(value, 1, 1, 1);
        }
        String local = value.substring(0, at);
        String domain = value.substring(at);
        String head = local.substring(0, 1);
        return head + "***" + domain;
    }

    private static String maskBankCard(String value) {
        int length = value.length();
        if (length <= 4) {
            return "*".repeat(Math.max(length, 1));
        }
        return "**** " + value.substring(length - 4);
    }
}
