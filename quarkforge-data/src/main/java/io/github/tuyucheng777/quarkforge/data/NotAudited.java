package io.github.tuyucheng777.quarkforge.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 标记实体不参与审计。例如审计日志自身必须跳过，避免递归记录。
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotAudited {
}
