package io.github.tuyucheng777.quarkforge.data;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

/// ## BaseEntity
/// 实体基类，内置：
///
/// - `id`：雪花 ID，首次持久化时自动赋值（节点 ID 由 `quarkforge.data.worker-id` 配置）
/// - `createdAt` / `updatedAt`：审计时间戳，由 JPA 生命周期回调自动维护
///
/// 子类直接继承即可获得 CRUD 能力：
///
/// ```java
/// @Entity
/// public class User extends BaseEntity {
///     public String name;
///
///     public static Optional<User> findByName(String name) {
///         return find("name", name).singleResultOptional();
///     }
/// }
/// ```
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {

    @Id
    @Column(updatable = false)
    public Long id;

    @Column(name = "created_at", updatable = false, nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = EntityIdGenerator.next();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
