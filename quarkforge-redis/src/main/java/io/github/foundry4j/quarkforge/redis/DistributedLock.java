package io.github.foundry4j.quarkforge.redis;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## DistributedLock
/// 注解式 Redis 分布式锁，加在方法或类上：
///
/// ```java
/// @DistributedLock(key = "order:pay:#orderId")
/// public void pay(long orderId) { ... }
///
/// @DistributedLock(key = "t:#req.tenantId:o:#req.id", waitSeconds = 3, leaseSeconds = 60)
/// public void submit(PayRequest req) { ... }
/// ```
///
/// - `key` 模板支持 `#参数名` / `#p0` 占位符与 `.字段` 链式取值（依赖编译期参数名保留），
///   留空时退化为 `类名#方法名`，同一方法全局互斥；
/// - 方法级注解优先于类级注解；
/// - 抢锁失败（含等待超时）抛出 `409 CONFLICT` 业务异常；
/// - 锁带租约自动过期，业务宕机不会形成死锁；解锁校验令牌，不会误删他人的锁。
@InterceptorBinding
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /// 锁键模板，支持 `#参数名` / `#p0` / `.字段` 占位符；留空使用方法签名。
    @Nonbinding
    String key() default "";

    /// 锁键前缀，用于区分业务域、避免与其他 Redis 键冲突。
    @Nonbinding
    String prefix() default "qf:lock:";

    /// 锁租约（秒），到期自动释放；取值小于 1 时按 1 秒处理。
    @Nonbinding
    long leaseSeconds() default 30;

    /// 抢锁等待时长（秒）：0 表示不等待、抢不到立即失败；
    /// 大于 0 时在期限内按固定间隔重试，超时仍失败抛 `409`。
    @Nonbinding
    long waitSeconds() default 0;
}
