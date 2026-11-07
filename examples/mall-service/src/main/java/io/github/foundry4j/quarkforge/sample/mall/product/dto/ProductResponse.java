package io.github.foundry4j.quarkforge.sample.mall.product.dto;

import io.github.foundry4j.quarkforge.sample.mall.product.Product;

import java.math.BigDecimal;

/// ## ProductResponse
/// 商品信息响应。
public record ProductResponse(Long id, String name, String description, BigDecimal price,
                              int stock, String status) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.id, product.name, product.description,
                product.price, product.stock, product.status.name());
    }
}
