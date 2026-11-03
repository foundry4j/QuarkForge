package io.github.tuyucheng777.quarkforge.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/// 测试用软删除实体。
@Entity
@Table(name = "qf_test_soft_note")
public class SoftNote extends SoftDeleteEntity {

    @Column(nullable = false, length = 128)
    public String title;
}
