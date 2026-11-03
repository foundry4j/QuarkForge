package io.github.tuyucheng777.quarkforge.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/// 测试用租户实体。
@Entity
@Table(name = "qf_test_tenant_note")
public class TenantNote extends TenantEntity {

    @Column(nullable = false, length = 128)
    public String title;
}
