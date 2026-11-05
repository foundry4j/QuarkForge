package io.github.foundry4j.quarkforge.dict.dto;

import io.github.foundry4j.quarkforge.dict.entity.DictItem;

import java.io.Serializable;

/// ## DictItemView
/// 字典项对外视图，也是缓存中的存储形态。
///
/// 该 record 可直接被 Jackson 序列化 / 反序列化，
/// 因此 Redis 两级缓存也无需额外包装类型。
///
/// @param value       存储值
/// @param label       展示名称
/// @param sort        排序值
/// @param parentValue 父级值
public record DictItemView(String value, String label, int sort, String parentValue) implements Serializable {

    public static DictItemView from(DictItem item) {
        return new DictItemView(item.itemValue, item.label, item.sortOrder, item.parentValue);
    }
}
