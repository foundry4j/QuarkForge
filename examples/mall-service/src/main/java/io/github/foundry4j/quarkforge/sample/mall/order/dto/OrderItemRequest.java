package io.github.foundry4j.quarkforge.sample.mall.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/// ## OrderItemRequest
/// 下单明细行。
public record OrderItemRequest(

        @NotNull(message = "不能为空")
        Long productId,

        @Min(value = 1, message = "必须大于 0")
        int quantity
) {
}
