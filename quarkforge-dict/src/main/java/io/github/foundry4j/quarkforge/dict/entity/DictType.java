package io.github.foundry4j.quarkforge.dict.entity;

import io.github.foundry4j.quarkforge.data.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/// ## DictType
/// 字典类型（元数据）。每个租户下 `code` 唯一，例如 `gender`、`order_status`。
///
/// 字典项存于 [DictItem]，按 `typeCode` 关联；字典类型被删除时其下字典项一并删除。
@Entity
@Table(name = "qf_dict_type",
        uniqueConstraints = @UniqueConstraint(name = "uk_qf_dict_type_code", columnNames = {"tenant_id", "code"}))
public class DictType extends TenantEntity {

    /// 字典编码，业务代码引用此值，创建后不可修改。
    @Column(nullable = false, length = 64)
    public String code;

    /// 字典名称，用于界面展示。
    @Column(nullable = false, length = 128)
    public String name;

    /// 备注说明。
    @Column(length = 512)
    public String description;

    /// 是否启用；停用后新增引用会受限，但已存数据仍可翻译。
    @Column(nullable = false)
    public boolean enabled = true;
}
