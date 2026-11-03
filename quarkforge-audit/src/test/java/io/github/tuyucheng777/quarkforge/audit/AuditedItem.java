package io.github.tuyucheng777.quarkforge.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.github.tuyucheng777.quarkforge.data.BaseEntity;

/// 测试用被审计实体。
@Entity
@Table(name = "qf_test_audited_item")
public class AuditedItem extends BaseEntity {

    @Column(nullable = false, length = 64)
    public String name;

    @Column(nullable = false)
    public int price;
}
