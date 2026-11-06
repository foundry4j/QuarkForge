package io.github.foundry4j.quarkforge.test;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## CleanDatabase
/// 标在 `@QuarkusTest` 测试类上，每个测试方法执行前
/// 对持久化单元的全部实体表执行批量删除，保证用例间数据隔离：
///
/// ```java
/// @QuarkusTest
/// @CleanDatabase
/// class UserRepositoryTest { ... }
/// ```
///
/// 实现是对 Metamodel 中全部实体逐个执行 `DELETE FROM Entity` 的 HQL 批量删除，
/// 与具体数据库无关；但**不处理外键级联顺序**——实体间有外键约束时
/// 可能因删除顺序冲突失败，此类场景建议改为自建清理 SQL 或依赖
/// `@TestTransaction` 回滚。
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DatabaseCleanupExtension.class)
public @interface CleanDatabase {
}
