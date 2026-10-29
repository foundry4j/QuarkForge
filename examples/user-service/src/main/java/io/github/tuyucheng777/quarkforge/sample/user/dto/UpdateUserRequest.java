package io.github.tuyucheng777.quarkforge.sample.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import io.github.tuyucheng777.quarkforge.sample.user.UserStatus;

/// ## UpdateUserRequest
/// 更新用户请求体，字段为 `null` 表示不修改。
public record UpdateUserRequest(

        @Size(max = 64, message = "长度不能超过 64")
        String displayName,

        @Email(message = "格式不正确")
        @Size(max = 128, message = "长度不能超过 128")
        String email,

        UserStatus status
) {
}
