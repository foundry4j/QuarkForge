package io.github.foundry4j.quarkforge.excel;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/// ## ExcelImportSpec
/// 导入配置：行映射、逐行校验、成功行消费与错误上限。
///
/// ```java
/// ExcelImportSpec<User> spec = ExcelImportSpec.<User>builder()
///         .mapper(row -> new User(row.cell("id"), row.cell("name")))
///         .validator((user, ctx) -> {
///             if (user.name().isBlank()) {
///                 ctx.reject("name", user.name(), "名称不能为空");
///             }
///         })
///         .rowSink(users::add)
///         .build();
/// ```
///
/// @param sheetIndex     Excel 工作表下标
/// @param headerRowIndex 表头行下标（0 起始），`-1` 表示无表头
/// @param maxErrors      最多保留的错误条数；`0` 表示不限制（注意大文件可能占用较多内存）
/// @param skipEmptyRows  是否跳过空行
/// @param charset        CSV 编码
/// @param mapper         行映射器（必填）
/// @param validators     逐行校验器
/// @param rowSink        成功行消费者；为 `null` 时仅统计不保留
public record ExcelImportSpec<T>(
        int sheetIndex,
        int headerRowIndex,
        int maxErrors,
        boolean skipEmptyRows,
        Charset charset,
        RowMapper<T> mapper,
        List<RowValidator<T>> validators,
        Consumer<T> rowSink) {

    public ExcelImportSpec {
        if (mapper == null) {
            throw new IllegalArgumentException("mapper 不能为空");
        }
        if (sheetIndex < 0) {
            throw new IllegalArgumentException("sheetIndex 不能为负数");
        }
        if (headerRowIndex < -1) {
            throw new IllegalArgumentException("headerRowIndex 最小为 -1");
        }
        if (maxErrors < 0) {
            throw new IllegalArgumentException("maxErrors 不能为负数");
        }
        charset = charset == null ? StandardCharsets.UTF_8 : charset;
        validators = validators == null ? List.of() : List.copyOf(validators);
    }

    /// 转换为底层读取选项。
    public RowReadOptions rowReadOptions() {
        return new RowReadOptions(sheetIndex, headerRowIndex, skipEmptyRows, charset);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {

        private int sheetIndex;
        private int headerRowIndex;
        private int maxErrors = 500;
        private boolean skipEmptyRows = true;
        private Charset charset = StandardCharsets.UTF_8;
        private RowMapper<T> mapper;
        private final List<RowValidator<T>> validators = new ArrayList<>();
        private Consumer<T> rowSink;

        /// 指定工作表下标，默认 0。
        public Builder<T> sheet(int sheetIndex) {
            this.sheetIndex = sheetIndex;
            return this;
        }

        /// 指定表头行下标，默认 0；`-1` 表示无表头。
        public Builder<T> headerRow(int headerRowIndex) {
            this.headerRowIndex = headerRowIndex;
            return this;
        }

        /// 最多保留的错误条数，默认 500；`0` 表示不限制。
        public Builder<T> maxErrors(int maxErrors) {
            this.maxErrors = maxErrors;
            return this;
        }

        /// 是否跳过空行，默认 `true`。
        public Builder<T> skipEmptyRows(boolean skipEmptyRows) {
            this.skipEmptyRows = skipEmptyRows;
            return this;
        }

        /// CSV 编码，默认 UTF-8。
        public Builder<T> charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public Builder<T> mapper(RowMapper<T> mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder<T> validator(RowValidator<T> validator) {
            if (validator != null) {
                this.validators.add(validator);
            }
            return this;
        }

        public Builder<T> validators(Collection<RowValidator<T>> validators) {
            if (validators != null) {
                this.validators.addAll(validators);
            }
            return this;
        }

        /// 成功行的消费者（例如写入数据库或批量缓冲）。
        public Builder<T> rowSink(Consumer<T> rowSink) {
            this.rowSink = rowSink;
            return this;
        }

        public ExcelImportSpec<T> build() {
            return new ExcelImportSpec<>(sheetIndex, headerRowIndex, maxErrors, skipEmptyRows,
                    charset, mapper, validators, rowSink);
        }
    }
}
