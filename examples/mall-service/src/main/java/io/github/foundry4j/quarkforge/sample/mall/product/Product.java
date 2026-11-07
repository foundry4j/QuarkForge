package io.github.foundry4j.quarkforge.sample.mall.product;

import io.github.foundry4j.quarkforge.data.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/// ## Product
/// 商品实体，库存直接保存在商品上。
@Entity
@Table(name = "qf_mall_product",
        indexes = @Index(name = "idx_qf_mall_product_name", columnList = "name"))
public class Product extends BaseEntity {

    /// 商品名称。
    @Column(nullable = false, length = 128)
    public String name;

    /// 商品描述。
    @Column(length = 512)
    public String description;

    /// 单价。
    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal price;

    /// 库存数量。
    @Column(nullable = false)
    public int stock;

    /// 商品状态。
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public ProductStatus status = ProductStatus.ON_SALE;
}
