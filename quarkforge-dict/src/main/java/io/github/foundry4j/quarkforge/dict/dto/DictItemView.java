package io.github.foundry4j.quarkforge.dict.dto;

import java.io.Serializable;

/// ## DictItemView
/// 字典项对外视图，也是缓存中的存储形态。
///
/// 该 record 可直接被 Jackson 序列化 / 反序列化，
/// 因此 Redis 两级缓存也无需额外包装类型。
/// 与实体之间的映射由 MapStruct 生成的 [io.github.foundry4j.quarkforge.dict.mapper.DictMapper] 完成。
///
/// @param value       存储值
/// @param label       展示名称
/// @param sort        排序值
/// @param parentValue 父级值
public record DictItemView(String value, String label, int sort, String parentValue) implements Serializable {
}
