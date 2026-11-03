package io.github.foundry4j.quarkforge.crypto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/// ## Sensitive
/// 标记敏感字段，JSON 序列化时自动脱敏（数据库仍保存原始值）。
///
/// ```java
/// @Sensitive(SensitiveType.PHONE)
/// public String phone;   // 接口输出: "138****0000"
/// ```
///
/// 如需同时加密存储，叠加 [EncryptedStringConverter]：
///
/// ```java
/// @Convert(converter = EncryptedStringConverter.class)
/// @Sensitive(SensitiveType.PHONE)
/// public String phone;
/// ```
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
public @interface Sensitive {

    /// 脱敏类型，默认保留首尾字符。
    SensitiveType value() default SensitiveType.DEFAULT;
}
