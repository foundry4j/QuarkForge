package io.github.foundry4j.quarkforge.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/// ## DictItemRequest
/// 新建 / 更新字典项的请求体。
///
/// @param label       展示名称
/// @param value       存储值
/// @param sort        排序值，升序展示，`null` 视为 0
/// @param enabled     是否启用，`null` 视为启用
/// @param parentValue 父级字典项的值，层级字典使用
/// @param remark      备注说明
public record DictItemRequest(
        @NotBlank @Size(max = 128) String label,
        @NotBlank @Size(max = 128) String value,
        Integer sort,
        Boolean enabled,
        @Size(max = 128) String parentValue,
        @Size(max = 512) String remark
) {
}
