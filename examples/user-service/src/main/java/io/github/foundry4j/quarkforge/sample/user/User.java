package io.github.foundry4j.quarkforge.sample.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import io.github.foundry4j.quarkforge.data.BaseEntity;

/// ## User
/// 用户实体。继承 [BaseEntity] 后自动获得雪花主键与审计字段。
@Entity
@Table(name = "qf_user", indexes = @Index(name = "idx_qf_user_username", columnList = "username", unique = true))
public class User extends BaseEntity {

    /// 登录名，全局唯一。
    @Column(nullable = false, length = 64)
    public String username;

    /// 展示名。
    @Column(nullable = false, length = 64)
    public String displayName;

    /// 邮箱，允许为空。
    @Column(length = 128)
    public String email;

    /// 用户状态。
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public UserStatus status = UserStatus.ACTIVE;

    /// 授权角色，签发 JWT 时写入 `groups` claim。
    @Column(nullable = false, length = 32)
    public String role = "user";

    /// 密码哈希（PBKDF2-SHA256 十六进制串）。
    @Column(nullable = false, length = 128)
    public String passwordHash;

    /// 密码盐值（16 字节随机数的 Base64 编码）。
    @Column(nullable = false, length = 32)
    public String passwordSalt;
}
