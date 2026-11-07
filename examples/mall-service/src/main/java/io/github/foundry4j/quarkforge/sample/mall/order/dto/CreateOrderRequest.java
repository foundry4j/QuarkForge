package io.github.foundry4j.quarkforge.sample.mall.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/// ## CreateOrderRequest
/// 创建订单请求体。`requestNo` 由客户端生成，用于保证重复提交的幂等性。
public record CreateOrderRequest(

        @NotBlank(message = "不能为空")
        @Size(max = 64, message = "长度不能超过 64")
        String requestNo,

        @NotEmpty(message = "不能为空")
        @Size(max = 50, message = "最多 50 行")
        List<@Valid OrderItemRequest> items
) {
}
