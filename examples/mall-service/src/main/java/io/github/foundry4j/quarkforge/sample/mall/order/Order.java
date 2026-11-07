package io.github.foundry4j.quarkforge.sample.mall.order;

import io.github.foundry4j.quarkforge.data.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/// ## Order
/// 订单主表。
@Entity
@Table(name = "qf_mall_order",
        indexes = {
                @Index(name = "idx_qf_mall_order_user", columnList = "user_id"),
                @Index(name = "idx_qf_mall_order_no", columnList = "order_no", unique = true),
                @Index(name = "idx_qf_mall_order_request", columnList = "request_no", unique = true)
        })
public class Order extends BaseEntity {

    /// 订单号。
    @Column(name = "order_no", nullable = false, length = 64)
    public String orderNo;

    /// 客户端请求号，用于幂等与问题排查。
    @Column(name = "request_no", nullable = false, length = 64)
    public String requestNo;

    /// 下单用户 ID。
    @Column(name = "user_id", nullable = false)
    public Long userId;

    /// 订单总金额。
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    public BigDecimal totalAmount;

    /// 订单状态。
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public OrderStatus status = OrderStatus.CREATED;
}
