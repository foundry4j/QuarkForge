package io.github.foundry4j.quarkforge.crypto;

/// ## SensitiveType
/// 敏感数据类型，决定 JSON 输出时的脱敏规则。
public enum SensitiveType {

    /// 手机号：`13800000000` → `138****0000`
    PHONE,

    /// 身份证号：`110101199001011234` → `110101********1234`
    ID_CARD,

    /// 邮箱：`alice@example.com` → `a***@example.com`
    EMAIL,

    /// 银行卡号：`6222021234567890123` → `**** 0123`
    BANK_CARD,

    /// 姓名：`张三` → `张*`
    NAME,

    /// 密码：始终输出 `******`
    PASSWORD,

    /// 通用规则：保留首尾字符，中间全部打码
    DEFAULT
}
