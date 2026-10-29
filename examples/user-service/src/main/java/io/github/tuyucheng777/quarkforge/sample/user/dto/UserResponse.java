package io.github.tuyucheng777.quarkforge.sample.user.dto;

import java.time.Instant;

import io.github.tuyucheng777.quarkforge.sample.user.User;
import io.github.tuyucheng777.quarkforge.sample.user.UserStatus;

/// ## UserResponse
/// 用户对外视图，仅暴露安全字段（不含密码相关信息）。
public record UserResponse(
        Long id,
        String username,
        String displayName,
        String email,
        UserStatus status,
        String role,
        Instant createdAt,
        Instant updatedAt
) {

    /// 由实体转换为对外视图。
    public static UserResponse from(User user) {
        return new UserResponse(user.id, user.username, user.displayName, user.email,
                user.status, user.role, user.createdAt, user.updatedAt);
    }
}
