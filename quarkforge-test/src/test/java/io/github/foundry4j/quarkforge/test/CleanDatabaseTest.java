package io.github.foundry4j.quarkforge.test;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// ## CleanDatabaseTest
/// 第一个用例写入数据，第二个用例验证已被扩展清空。
@QuarkusTest
@CleanDatabase
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CleanDatabaseTest {

    @Inject
    EntityManager em;

    @Entity(name = "CleanProbe")
    public static class CleanProbe {

        @Id
        @GeneratedValue
        public Long id;

        public String name;

        public CleanProbe() {
        }

        public CleanProbe(String name) {
            this.name = name;
        }
    }

    @Test
    @Order(1)
    @TestTransaction
    void seedRow() {
        em.persist(new CleanProbe("a"));
        assertEquals(1, count());
    }

    @Test
    @Order(2)
    @TestTransaction
    void previousDataWasCleaned() {
        assertEquals(0, count());
    }

    private long count() {
        return em.createQuery("SELECT COUNT(p) FROM CleanProbe p", Long.class).getSingleResult();
    }
}
