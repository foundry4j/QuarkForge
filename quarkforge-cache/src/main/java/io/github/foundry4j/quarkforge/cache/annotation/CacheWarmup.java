package io.github.foundry4j.quarkforge.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## CacheWarmup
/// 启动预热注解。应用启动完成后（Quarkus `StartupEvent`），框架会依次调用所有标注此注解的
/// **无参方法**，用于提前加载热点数据，避免首批请求集中击穿缓存。
///
/// 方法内部可以通过 [Cacheable] 或 `CacheService` 写入缓存，框架只负责在启动后触发调用：
///
/// ```java
/// @ApplicationScoped
/// public class ProductWarmup {
///
///     @Inject ProductService products;
///
///     @CacheWarmup(name = "hot-products", order = 100)
///     public void warm() {
///         products.hotIds().forEach(products::requireById);
///     }
/// }
/// ```
///
/// 说明：
///
/// - 方法必须无参，返回值会被忽略
/// - 调用经由 CDI 代理，`@Cacheable` / `@Transactional` 等拦截器正常生效
/// - 单个任务失败默认只记录日志并继续执行后续任务；`quarkforge.cache.warmup.fail-fast=true`
///   可让失败中断启动
/// - 全局开关：`quarkforge.cache.warmup.enabled=false`
///
/// 注意：生产构建下 Quarkus 会移除「未被注入」的 Bean。若预热 Bean 只被本注解引用，
/// 请同时标注 `@io.quarkus.arc.Unremovable`（开发模式与测试环境不会移除）。
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheWarmup {

    /// 任务名称，仅用于日志；留空时使用「类名#方法名」。
    String name() default "";

    /// 执行顺序，数值越小越先执行；相同值按 Bean 发现顺序执行。
    int order() default 0;

    /// 是否启用本任务。
    boolean enabled() default true;
}
