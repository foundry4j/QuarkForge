package io.github.foundry4j.quarkforge.cache.annotation;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## CacheEvict
/// 声明式缓存失效注解：写操作（create / update / delete）后
/// 从缓存中移除旧值，保持缓存与数据库一致。
///
/// ```java
/// @CacheEvict(key = "#p0", namespace = "user")
/// public void updateUser(long id, UserPatch patch) { ... }
/// ```
///
/// 标注在写方法上，默认在方法**成功返回后**执行失效；
/// 要在方法执行前失效（例如先删再写），设 `beforeInvocation = true`。
@Inherited
@Documented
@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CacheEvict.List.class)
public @interface CacheEvict {

    /// 失效键表达式，语法同 [Cacheable.key]。
    @Nonbinding
    String key() default "";

    /// 缓存键 namespace 前缀。
    @Nonbinding
    String namespace() default "";

    /// `true`：方法执行前失效；默认 `false`（成功返回后失效）。
    @Nonbinding
    boolean beforeInvocation() default false;

    @Documented
    @Inherited
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        CacheEvict[] value();
    }
}
