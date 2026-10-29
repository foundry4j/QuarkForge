package io.github.tuyucheng777.quarkforge.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/// ## RateLimit
/// 声明式限流注解，标注在资源方法或类上：
///
/// ```java
/// @POST
/// @RateLimit(permits = 5, windowSeconds = 60, key = KeyStrategy.USER)
/// public Result<Void> sendSmsCode() { ... }
/// ```
///
/// 超出额度时抛出 `RateLimitException`，由 web 层映射为 HTTP 429。
@Inherited
@Documented
@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /// 时间窗口内允许的最大请求数。
    @Nonbinding
    int permits();

    /// 时间窗口长度（秒），默认 60 秒。
    @Nonbinding
    long windowSeconds() default 60;

    /// 限流维度，默认按登录用户。
    @Nonbinding
    KeyStrategy key() default KeyStrategy.USER;
}
