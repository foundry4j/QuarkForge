package io.github.tuyucheng777.quarkforge.sample.user.dto;

/// ## LoginResponse
/// 登录成功响应。
///
/// @param accessToken      访问令牌
/// @param tokenType        令牌类型，固定 `Bearer`
/// @param expiresInSeconds 有效期（秒）
public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
