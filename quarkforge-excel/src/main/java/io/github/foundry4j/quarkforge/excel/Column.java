package io.github.foundry4j.quarkforge.excel;

import java.util.function.Function;

/// ## Column
/// 导出一列的声明：表头、取值函数与建议列宽。
///
/// ```java
/// Column.of("用户ID", User::id)
/// ```
public record Column<T>(String header, Function<T, Object> getter, int width) {

    public Column {
        if (getter == null) {
            throw new IllegalArgumentException("getter 不能为空");
        }
        if (header == null) {
            header = "";
        }
        if (width <= 0) {
            width = 20;
        }
    }

    public Column(String header, Function<T, Object> getter) {
        this(header, getter, 20);
    }

    public static <T> Column<T> of(String header, Function<T, Object> getter) {
        return new Column<>(header, getter, 20);
    }

    public static <T> Column<T> of(String header, Function<T, Object> getter, int width) {
        return new Column<>(header, getter, width);
    }
}
