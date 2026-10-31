package io.github.tuyucheng777.quarkforge.idempotency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/// ## Idempotent
/// 声明式幂等注解，标注在方法或类上：
///
/// ```java
/// @POST
/// @Idempotent(key = "order:#req.orderNo", ttlSeconds = 600)
/// public Result<OrderVO> createOrder(@RequestBody CreateOrderRequest req) { ... }
/// ```
///
/// 同一幂等键的重复请求：
/// - 首次请求执行业务方法，并将返回值缓存 `ttlSeconds` 秒；
/// - 缓存期内的重复请求直接返回首次结果，不再执行业务方法；
/// - 若相同键的请求尚在处理中，抛出 `BizException(CONFLICT)`。
///
/// `key` 支持 `#参数名.字段` 占位符，也支持 `#p0` 按索引取值；未配置时使用全部参数的序列化内容作为键。
@Inherited
@Documented
@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /// 幂等键表达式，如 `order:#req.orderNo`。留空时按参数序列化结果生成。
    @Nonbinding
    String key() default "";

    /// 结果缓存时长（秒），默认 1 天。
    @Nonbinding
    long ttlSeconds() default 86_400L;
}
