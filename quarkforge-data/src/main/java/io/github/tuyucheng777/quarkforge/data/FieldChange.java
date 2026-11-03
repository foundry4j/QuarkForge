package io.github.tuyucheng777.quarkforge.data;

import java.io.Serial;
import java.io.Serializable;

/// 一次审计事件中的字段变更：字段名、旧值、新值。
public record FieldChange(String field, String oldValue, String newValue) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public FieldChange {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field 不能为空");
        }
    }
}
