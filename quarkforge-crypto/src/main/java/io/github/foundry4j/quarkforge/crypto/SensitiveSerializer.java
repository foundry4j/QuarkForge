package io.github.foundry4j.quarkforge.crypto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/// ## SensitiveSerializer
/// 读取字段上的 [Sensitive] 配置并按 [SensitiveType] 输出脱敏后的字符串。
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final SensitiveType type;

    public SensitiveSerializer() {
        this(SensitiveType.DEFAULT);
    }

    public SensitiveSerializer(SensitiveType type) {
        this.type = type == null ? SensitiveType.DEFAULT : type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(Masking.mask(value, type));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        Sensitive annotation = property == null ? null : property.getAnnotation(Sensitive.class);
        if (annotation == null && property != null) {
            annotation = property.getContextAnnotation(Sensitive.class);
        }
        return new SensitiveSerializer(annotation == null ? SensitiveType.DEFAULT : annotation.value());
    }
}
