package io.github.foundry4j.quarkforge.sample.mall.user.dto;

/// ## LoginResponse
/// 登录成功后的令牌信息。
public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
