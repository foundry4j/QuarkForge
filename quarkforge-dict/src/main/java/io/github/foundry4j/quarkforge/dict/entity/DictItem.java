package io.github.foundry4j.quarkforge.dict.entity;

import io.github.foundry4j.quarkforge.data.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/// ## DictItem
/// 字典项。同一字典类型下 `itemValue` 唯一；`sortOrder` 决定展示顺序。
///
/// `parentValue` 支持层级字典（如省-市、分类-子分类），
/// 传 `null` 表示顶级项。
@Entity
@Table(name = "qf_dict_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_qf_dict_item_value", columnNames = {"tenant_id", "type_code", "item_value"}),
        indexes = @Index(name = "idx_qf_dict_item_type", columnList = "type_code, sort_order"))
public class DictItem extends TenantEntity {

    /// 所属字典类型编码。
    @Column(name = "type_code", nullable = false, length = 64)
    public String typeCode;

    /// 展示名称，如「男」「已完成」。
    @Column(nullable = false, length = 128)
    public String label;

    /// 存储值，业务表中保存的是这个值。
    @Column(name = "item_value", nullable = false, length = 128)
    public String itemValue;

    /// 排序值，升序展示。
    @Column(name = "sort_order", nullable = false)
    public int sortOrder;

    /// 是否启用；停用项不出现在默认列表中，但仍可被翻译。
    @Column(nullable = false)
    public boolean enabled = true;

    /// 父级字典项的值，用于层级字典。
    @Column(name = "parent_value", length = 128)
    public String parentValue;

    /// 备注说明。
    @Column(length = 512)
    public String remark;
}
