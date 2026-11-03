package io.github.foundry4j.quarkforge.web.version;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 声明接口支持的 API 版本。可标注在资源类（对该类所有方法生效）或
/// 单个方法上，方法级配置优先。
///
/// 调用方通过请求头 `X-API-Version`（或 URL 首段 `/v1/...`）指定版本；
/// 未声明本注解的接口不做版本校验，向后兼容。
///
/// ```java
/// @Path("/orders")
/// @ApiVersion("v1")
/// public class OrderResource {
///
///     @GET
///     public List<OrderVO> list() { ... }
///
///     @GET
///     @Path("/legacy")
///     @ApiVersion(value = "v1", deprecated = true, sunset = "2027-01-01")
///     public List<OrderVO> legacyList() { ... }
/// }
/// ```
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ApiVersion {

    /// 支持的版本列表，例如 `{"v1", "v2"}`。
    String[] value();

    /// 是否已废弃。命中废弃接口时响应会带上 `Deprecation: true`。
    boolean deprecated() default false;

    /// 计划下线日期（ISO-8601，如 `2027-01-01`），写入 `Sunset` 响应头。
    String sunset() default "";

    /// 替代接口路径，写入 `Link: <...>; rel="successor-version"`。
    String successor() default "";
}
