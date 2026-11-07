package io.github.foundry4j.quarkforge.sample.mall.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/// ## CreateProductRequest
/// 新增商品请求体。
public record CreateProductRequest(

        @NotBlank(message = "不能为空")
        @Size(max = 128, message = "长度不能超过 128")
        String name,

        @Size(max = 512, message = "长度不能超过 512")
        String description,

        @NotNull(message = "不能为空")
        @DecimalMin(value = "0.01", message = "必须大于 0")
        @Digits(integer = 10, fraction = 2, message = "整数位最多 10 位，小数位最多 2 位")
        BigDecimal price,

        @Min(value = 0, message = "不能小于 0")
        int stock
) {
}
