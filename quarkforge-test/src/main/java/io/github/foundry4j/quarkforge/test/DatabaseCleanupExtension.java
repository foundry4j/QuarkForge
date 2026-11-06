package io.github.foundry4j.quarkforge.test;

import io.quarkus.arc.Arc;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Set;

/// ## DatabaseCleanupExtension
/// [CleanDatabase] 携带的 JUnit 扩展：每个测试方法前开启独立事务，
/// 对 Metamodel 中全部实体执行 HQL 批量删除。
///
/// 通过 Arc 在运行时解析 `EntityManager` / `UserTransaction`，
/// 因此只在 `@QuarkusTest` 环境下有效。
public class DatabaseCleanupExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        EntityManager em = Arc.container().instance(EntityManager.class).get();
        UserTransaction tx = Arc.container().instance(UserTransaction.class).get();

        tx.begin();
        try {
            Set<EntityType<?>> entities = em.getMetamodel().getEntities();
            for (EntityType<?> entity : entities) {
                em.createQuery("DELETE FROM " + entity.getName()).executeUpdate();
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.getStatus() == Status.STATUS_ACTIVE) {
                tx.rollback();
            }
            throw new IllegalStateException("测试数据库清理失败", e);
        }
    }
}
