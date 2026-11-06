package io.github.foundry4j.quarkforge.excel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/// ## ExcelExportSpec
/// 导出配置：工作表名、列定义与流式写出参数。
///
/// ```java
/// ExcelExportSpec<User> spec = ExcelExportSpec.<User>builder("Users")
///         .column("ID", User::id, 12)
///         .column("名称", User::name, 24)
///         .build();
/// ExcelExporter.write(out, ExcelFormat.XLSX, spec, users);
/// ```
///
/// @param sheetName    工作表名
/// @param columns      列定义
/// @param windowSize   Excel 滑动窗口行数，越大内存占用越高，默认 100
/// @param freezeHeader 是否冻结表头
/// @param csvBom       CSV 是否写入 UTF-8 BOM（Excel 打开中文 CSV 需要）
public record ExcelExportSpec<T>(String sheetName, List<Column<T>> columns,
                                 int windowSize, boolean freezeHeader, boolean csvBom) {

    public ExcelExportSpec {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("columns 不能为空");
        }
        columns = List.copyOf(columns);
        if (sheetName == null || sheetName.isBlank()) {
            sheetName = "Sheet1";
        }
        if (windowSize <= 0) {
            windowSize = 100;
        }
    }

    public static <T> Builder<T> builder(String sheetName) {
        return new Builder<>(sheetName);
    }

    public static final class Builder<T> {

        private final String sheetName;
        private final List<Column<T>> columns = new ArrayList<>();
        private int windowSize = 100;
        private boolean freezeHeader = true;
        private boolean csvBom = true;

        private Builder(String sheetName) {
            this.sheetName = sheetName;
        }

        public Builder<T> column(String header, Function<T, Object> getter) {
            columns.add(Column.of(header, getter));
            return this;
        }

        public Builder<T> column(String header, Function<T, Object> getter, int width) {
            columns.add(Column.of(header, getter, width));
            return this;
        }

        public Builder<T> column(Column<T> column) {
            columns.add(column);
            return this;
        }

        /// Excel 滑动窗口行数，默认 100。
        public Builder<T> windowSize(int windowSize) {
            this.windowSize = windowSize;
            return this;
        }

        /// 是否冻结表头，默认 `true`。
        public Builder<T> freezeHeader(boolean freezeHeader) {
            this.freezeHeader = freezeHeader;
            return this;
        }

        /// CSV 是否写 UTF-8 BOM，默认 `true`。
        public Builder<T> csvBom(boolean csvBom) {
            this.csvBom = csvBom;
            return this;
        }

        public ExcelExportSpec<T> build() {
            return new ExcelExportSpec<>(sheetName, columns, windowSize, freezeHeader, csvBom);
        }
    }
}
