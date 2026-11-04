package io.github.foundry4j.quarkforge.sample.order.dto;

import io.github.foundry4j.quarkforge.sample.order.Order;
import io.github.foundry4j.quarkforge.sample.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/// ## OrderResponse
/// 订单对外视图。
public record OrderResponse(
        Long id,
        Long userId,
        String productName,
        int quantity,
        BigDecimal amount,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.id, order.userId, order.productName, order.quantity,
                order.amount, order.status, order.createdAt, order.updatedAt);
    }
}
