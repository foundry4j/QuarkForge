package io.github.foundry4j.quarkforge.sample.order;

import io.github.foundry4j.quarkforge.data.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/// ## Order
/// 订单实体。`userId` 字段记录下单用户，用户是否存在由 gRPC 调用 user-service 校验。
@Entity
@Table(name = "qf_order", indexes = @Index(name = "idx_qf_order_user_id", columnList = "userId"))
public class Order extends BaseEntity {

    /// 下单用户 ID（来自 user-service）。
    @Column(nullable = false)
    public Long userId;

    /// 商品名称。
    @Column(nullable = false, length = 128)
    public String productName;

    /// 数量。
    @Column(nullable = false)
    public int quantity;

    /// 总金额。
    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal amount;

    /// 订单状态。
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public OrderStatus status = OrderStatus.CREATED;
}
