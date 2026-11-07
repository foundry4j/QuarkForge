package io.github.foundry4j.quarkforge.sample.mall.order;

import io.github.foundry4j.quarkforge.data.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/// ## OrderItem
/// 订单明细，记录下单时的商品名称与价格快照。
@Entity
@Table(name = "qf_mall_order_item",
        indexes = @Index(name = "idx_qf_mall_order_item_order", columnList = "order_id"))
public class OrderItem extends BaseEntity {

    /// 所属订单 ID。
    @Column(name = "order_id", nullable = false)
    public Long orderId;

    /// 商品 ID。
    @Column(name = "product_id", nullable = false)
    public Long productId;

    /// 下单时的商品名称。
    @Column(name = "product_name", nullable = false, length = 128)
    public String productName;

    /// 下单时的商品单价。
    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal price;

    /// 购买数量。
    @Column(nullable = false)
    public int quantity;

    /// 小计金额。
    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal subtotal;
}
