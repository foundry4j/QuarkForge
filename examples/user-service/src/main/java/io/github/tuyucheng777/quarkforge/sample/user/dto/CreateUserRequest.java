package io.github.tuyucheng777.quarkforge.sample.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/// ## CreateUserRequest
/// 创建用户请求体。
public record CreateUserRequest(

        /// 登录名，唯一。
        @NotBlank(message = "不能为空")
        @Size(max = 64, message = "长度不能超过 64")
        @Pattern(regexp = "[a-zA-Z0-9_-]+", message = "只允许字母、数字、下划线和连字符")
        String username,

        /// 展示名。
        @NotBlank(message = "不能为空")
        @Size(max = 64, message = "长度不能超过 64")
        String displayName,

        /// 邮箱，可为空。
        @Email(message = "格式不正确")
        @Size(max = 128, message = "长度不能超过 128")
        String email,

        /// 初始密码，至少 8 位。
        @NotBlank(message = "不能为空")
        @Size(min = 8, max = 64, message = "长度必须在 8 到 64 之间")
        String password,

        /// 角色，默认 `user`。
        @Pattern(regexp = "user|admin", message = "只能是 user 或 admin")
        String role
) {
}
