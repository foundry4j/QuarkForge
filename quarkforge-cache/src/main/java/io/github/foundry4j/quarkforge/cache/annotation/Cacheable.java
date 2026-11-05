package io.github.foundry4j.quarkforge.cache.annotation;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## Cacheable
/// 声明式缓存注解，搭配 [CacheEvict] 使用：
///
/// ```java
/// @Cacheable(key = "#p0", ttl = "PT10M")
/// public User getUserById(long id) { ... }
/// ```
///
/// 注解生效需要方法所在 bean 被 Arc 管理（`@ApplicationScoped` / `@Singleton` 等）。
@Inherited
@Documented
@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {

    /// 缓存键表达式，支持：
    ///
    /// - `'string literal'` — 字面量（用于常量键）
    /// - `#p0`, `#p1`, ... — 方法参数（等价于 `#args[0]` 等）
    /// - `#p0.field` — 参数的 Bean 属性
    /// - `#p0.method()` — 参数的零参方法
    /// - `#root.methodName` — 当前方法名
    /// - 省略 — 默认键 = 类全限定名 + `#` + 方法名 + args 哈希
    @Nonbinding
    String key() default "";

    /// 缓存 TTL（ISO-8601 Duration）；留空使用 `quarkforge.cache.local.default-ttl`。
    @Nonbinding
    String ttl() default "";

    /// 是否缓存 `null` 值（配合空值缓存 + Bloom 防穿透）。
    @Nonbinding
    boolean cacheNull() default true;

    /// 是否启用 Bloom 防穿透（仅本地缓存实现生效）。
    @Nonbinding
    boolean preventPenetration() default true;

    /// 是否启用 single-flight 防击穿（仅本地缓存实现生效）。
    @Nonbinding
    boolean preventBreakdown() default true;

    /// 缓存键的 namespace 前缀（避免不同模块的键互相污染）。
    @Nonbinding
    String namespace() default "";
}
