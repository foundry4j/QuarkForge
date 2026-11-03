package io.github.foundry4j.quarkforge.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## RawResponse
/// 标注在资源类或方法上，跳过统一响应体自动包装，返回值按原样输出。
/// 适用于文件下载、第三方回调、对接外部协议等场景。
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RawResponse {
}
