package io.github.foundry4j.quarkforge.sample.mall.user.dto;

import io.github.foundry4j.quarkforge.sample.mall.user.User;

/// ## UserResponse
/// 用户信息响应，不包含密码哈希与盐值。
public record UserResponse(Long id, String username, String displayName, String phone,
                           String role, String status) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id, user.username, user.displayName, user.phone,
                user.role, user.status.name());
    }
}
