package io.github.foundry4j.quarkforge.sample.mall.product.dto;

import io.github.foundry4j.quarkforge.sample.mall.product.ProductStatus;
import io.github.foundry4j.quarkforge.validation.annotation.EnumValid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/// ## UpdateProductRequest
/// 更新商品请求体，字段为空表示不修改。
public record UpdateProductRequest(

        @Size(max = 128, message = "长度不能超过 128")
        String name,

        @Size(max = 512, message = "长度不能超过 512")
        String description,

        @DecimalMin(value = "0.01", message = "必须大于 0")
        @Digits(integer = 10, fraction = 2, message = "整数位最多 10 位，小数位最多 2 位")
        BigDecimal price,

        @Min(value = 0, message = "不能小于 0")
        Integer stock,

        @EnumValid(ProductStatus.class)
        String status
) {
}
