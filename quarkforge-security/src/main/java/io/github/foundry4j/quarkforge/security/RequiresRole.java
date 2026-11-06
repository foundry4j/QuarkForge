package io.github.foundry4j.quarkforge.security;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## RequiresRole
/// 角色校验注解，加在资源方法或类上：
///
/// ```java
/// @RequiresRole("admin")                              // 需要 admin 角色
/// @RequiresRole({"admin", "ops"})                     // 任意其一（默认 ANY）
/// @RequiresRole(value = {"a", "b"}, logical = ALL)    // 必须全部具备
/// ```
///
/// 方法级注解优先于类级注解（方法上有角色注解时不再看类上的）。
/// 未登录访问抛出 `401 UNAUTHORIZED`，角色不足抛出 `403 FORBIDDEN`，
/// 由 web 层全局异常映射为统一 `Result` 响应。
@InterceptorBinding
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {

    /// 允许的角色名集合。
    @Nonbinding
    String[] value() default {};

    /// 多角色的组合方式，默认任意满足。
    @Nonbinding
    Logical logical() default Logical.ANY;
}
