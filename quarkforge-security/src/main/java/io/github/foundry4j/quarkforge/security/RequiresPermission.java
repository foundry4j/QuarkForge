package io.github.foundry4j.quarkforge.security;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## RequiresPermission
/// 权限校验注解，比角色更细粒度，加在资源方法或类上：
///
/// ```java
/// @RequiresPermission("user:create")                          // 单个权限
/// @RequiresPermission({"user:create", "user:audit"})          // 任意其一（默认 ANY）
/// @RequiresPermission(value = {"a", "b"}, logical = ALL)      // 必须全部具备
/// ```
///
/// 权限来自 JWT 的 `perms` claim（签发时由业务方根据角色展开写入，
/// 见 [JwtTokenService#issue(String, String, java.util.Set, java.util.Set)]）。
/// 方法级注解优先于类级注解。未登录抛出 `401`，权限不足抛出 `403`。
@InterceptorBinding
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    /// 允许的权限标识集合。
    @Nonbinding
    String[] value() default {};

    /// 多权限的组合方式，默认任意满足。
    @Nonbinding
    Logical logical() default Logical.ANY;
}
