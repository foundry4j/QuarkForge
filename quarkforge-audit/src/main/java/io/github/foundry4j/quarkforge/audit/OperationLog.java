package io.github.foundry4j.quarkforge.audit;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## OperationLog
/// 声明式操作日志注解，标注在方法或类上，每次调用都会落一条
/// [OperationLogEntry]：
///
/// ```java
/// @POST
/// @OperationLog(module = "order", action = "create", description = "创建订单")
/// public Order createOrder(@Valid CreateOrderRequest req) { ... }
/// ```
///
/// 记录内容包括：操作人、方法、参数（JSON）、成功与否、错误信息、耗时、
/// traceId，以及 web 场景下的请求方法、路径、客户端 IP 与 User-Agent。
///
/// 失败记录通过独立事务写入，业务回滚不会丢失操作日志。
/// `recordArgs = false` 可避免参数落库（如包含密码、令牌等敏感内容时）。
@Inherited
@Documented
@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /// 业务模块，留空时取方法所在类的简单类名。
    @Nonbinding
    String module() default "";

    /// 操作类型，如 `create` / `update` / `delete`，留空时取方法名。
    @Nonbinding
    String action() default "";

    /// 操作描述。
    @Nonbinding
    String description() default "";

    /// 是否记录方法参数，默认记录。
    @Nonbinding
    boolean recordArgs() default true;
}
