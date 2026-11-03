package io.github.foundry4j.quarkforge.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;

/// ## JwtTokenService
/// JWT 签发服务。签名密钥等配置沿用 SmallRye JWT 标准配置项：
///
/// ```properties
/// # HS256 对称密钥（至少 32 字节），或改为 smallrye.jwt.sign.key-location 使用密钥文件
/// smallrye.jwt.sign.key=${JWT_SIGN_KEY}
/// # 验签配置（与签名密钥保持一致）
/// smallrye.jwt.verify.key=${JWT_SIGN_KEY}
/// mp.jwt.verify.issuer=quarkforge
/// ```
///
/// 框架扩展配置：
///
/// | 配置 | 默认值 | 说明 |
/// |------|--------|------|
/// | `quarkforge.jwt.issuer` | `quarkforge` | 签发方（iss claim） |
/// | `quarkforge.jwt.ttl-minutes` | `120` | 令牌有效期（分钟） |
@ApplicationScoped
public class JwtTokenService {

    @ConfigProperty(name = "quarkforge.jwt.issuer", defaultValue = "quarkforge")
    String issuer;

    @ConfigProperty(name = "quarkforge.jwt.ttl-minutes", defaultValue = "120")
    long ttlMinutes;

    /// 签发令牌，`subject` 作为用户 ID（sub claim），`username` 写入 upn claim。
    public String issue(String subject, String username, Set<String> roles) {
        return issue(subject, username, roles, Map.of());
    }

    /// 签发令牌并附加自定义 claims。
    public String issue(String subject, String username, Set<String> roles, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        JwtClaimsBuilder builder = Jwt.issuer(issuer)
                .subject(subject)
                .upn(username)
                .groups(roles == null ? Set.of() : roles)
                .issuedAt(now.getEpochSecond())
                .expiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES).getEpochSecond());
        extraClaims.forEach(builder::claim);
        return builder.sign();
    }
}
