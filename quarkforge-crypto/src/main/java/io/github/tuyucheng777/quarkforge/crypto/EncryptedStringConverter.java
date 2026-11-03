package io.github.tuyucheng777.quarkforge.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

/// ## EncryptedStringConverter
/// JPA 字段加密转换器：写入数据库前加密，读出后自动解密。
///
/// ```java
/// @Convert(converter = EncryptedStringConverter.class)
/// @Sensitive(SensitiveType.PHONE)
/// public String phone;
/// ```
///
/// 数据库列建议至少 `VARCHAR(255)`，长度需容纳 Base64 密文。
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : crypto().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : crypto().decrypt(dbData);
    }

    private static CryptoService crypto() {
        InstanceHandle<CryptoService> handle = Arc.container().instance(CryptoService.class);
        if (!handle.isAvailable()) {
            throw new IllegalStateException("找不到 CryptoService，请确认已引入 quarkforge-crypto");
        }
        return handle.get();
    }
}
