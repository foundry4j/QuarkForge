package io.github.foundry4j.quarkforge.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/// ## DictTypeRequest
/// 新建 / 更新字典类型的请求体。
///
/// @param code        字典编码，创建后不可修改
/// @param name        字典名称
/// @param description 备注说明
/// @param enabled     是否启用，`null` 视为启用
public record DictTypeRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 512) String description,
        Boolean enabled
) {
}
