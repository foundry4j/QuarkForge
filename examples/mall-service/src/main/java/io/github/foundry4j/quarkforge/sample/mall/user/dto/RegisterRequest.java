package io.github.foundry4j.quarkforge.sample.mall.user.dto;

import io.github.foundry4j.quarkforge.validation.annotation.PasswordStrength;
import io.github.foundry4j.quarkforge.validation.annotation.Phone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/// ## RegisterRequest
/// 买家注册请求体。角色固定为 `BUYER`，管理员由启动预置数据创建。
public record RegisterRequest(

        /// 登录名，唯一。
        @NotBlank(message = "不能为空")
        @Size(max = 64, message = "长度不能超过 64")
        @Pattern(regexp = "[a-zA-Z0-9_-]+", message = "只允许字母、数字、下划线和连字符")
        String username,

        /// 展示名。
        @NotBlank(message = "不能为空")
        @Size(max = 64, message = "长度不能超过 64")
        String displayName,

        /// 手机号，可为空。
        @Phone
        String phone,

        /// 登录密码，至少 8 位且必须包含字母、数字和特殊字符。
        @NotBlank(message = "不能为空")
        @PasswordStrength(min = 8, max = 64, requireSpecial = true,
                message = "长度至少 8 位且必须包含字母、数字和特殊字符")
        String password
) {
}
