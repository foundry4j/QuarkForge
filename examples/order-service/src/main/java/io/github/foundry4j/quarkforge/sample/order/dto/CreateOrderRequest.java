package io.github.foundry4j.quarkforge.sample.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/// ## CreateOrderRequest
/// 创建订单请求。
public record CreateOrderRequest(
        /// 下单用户 ID（必须是 user-service 中存在的用户）。
        @NotNull Long userId,
        @NotBlank String productName,
        @Min(1) int quantity,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
