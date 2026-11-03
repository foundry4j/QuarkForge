package io.github.foundry4j.quarkforge.sample.user.dto;

import jakarta.validation.constraints.NotBlank;

/// ## LoginRequest
/// 登录请求体。
public record LoginRequest(

        @NotBlank(message = "不能为空")
        String username,

        @NotBlank(message = "不能为空")
        String password
) {
}
