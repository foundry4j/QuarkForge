package io.github.foundry4j.quarkforge.sample.mall.order.dto;

import io.github.foundry4j.quarkforge.sample.mall.order.Order;
import io.github.foundry4j.quarkforge.sample.mall.order.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/// ## OrderResponse
/// 订单响应，包含订单明细。
public record OrderResponse(Long id, String orderNo, Long userId, BigDecimal totalAmount,
                            String status, Instant createdAt, List<OrderItemResponse> items) {

    public static OrderResponse from(Order order, List<OrderItem> items) {
        return new OrderResponse(order.id, order.orderNo, order.userId, order.totalAmount,
                order.status.name(), order.createdAt,
                items.stream().map(OrderItemResponse::from).toList());
    }
}
