package io.github.foundry4j.quarkforge.sample.mall.order.dto;

import io.github.foundry4j.quarkforge.sample.mall.order.OrderItem;

import java.math.BigDecimal;

/// ## OrderItemResponse
/// 订单明细响应。
public record OrderItemResponse(Long productId, String productName, BigDecimal price,
                                int quantity, BigDecimal subtotal) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.productId, item.productName, item.price,
                item.quantity, item.subtotal);
    }
}
