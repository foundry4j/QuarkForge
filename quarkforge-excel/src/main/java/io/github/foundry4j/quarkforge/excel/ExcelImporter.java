package io.github.foundry4j.quarkforge.excel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/// ## ExcelImporter
/// 流式导入：逐行映射 + 逐行校验 + 错误行回传。
///
/// 单行映射/校验失败不会中断整个文件，失败行以 [RowError] 形式返回；
/// 成功行通过 [ExcelImportSpec#rowSink()] 逐条消费，不在内存中累积。
///
/// ```java
/// List<User> users = new ArrayList<>();
/// ImportResult result = ExcelImporter.importData(in, ExcelFormat.XLSX,
///         ExcelImportSpec.<User>builder()
///                 .mapper(row -> new User(row.cell("id"), row.cell("name")))
///                 .validator((user, ctx) -> {
///                     if (user.name().isBlank()) {
///                         ctx.reject("name", user.name(), "名称不能为空");
///                     }
///                 })
///                 .rowSink(users::add)
///                 .build());
/// ```
public final class ExcelImporter {

    private ExcelImporter() {
    }

    /// 按文件名后缀识别格式后导入。
    public static <T> ImportResult importData(InputStream in, String filename, ExcelImportSpec<T> spec) {
        return importData(in, ExcelFormat.fromFilename(filename), spec);
    }

    /// 流式导入并返回计数与错误行。
    public static <T> ImportResult importData(InputStream in, ExcelFormat format, ExcelImportSpec<T> spec) {
        if (spec == null) {
            throw new IllegalArgumentException("spec 不能为空");
        }

        long[] counters = new long[2]; // [0] total, [1] success
        long[] failed = new long[1];
        List<RowError> errors = new ArrayList<>();
        boolean[] truncated = new boolean[1];

        ExcelReader.read(in, format, spec.rowReadOptions(), row -> {
            counters[0]++;
            RowContext context = new RowContext(row);
            T value = null;

            try {
                value = spec.mapper().map(row);
                if (value == null && !context.hasErrors()) {
                    context.reject("映射结果为空");
                }
            } catch (Exception e) {
                context.reject("映射失败: " + message(e));
            }

            if (value != null && !context.hasErrors()) {
                for (RowValidator<T> validator : spec.validators()) {
                    try {
                        validator.validate(value, context);
                    } catch (Exception e) {
                        context.reject("校验失败: " + message(e));
                    }
                }
            }

            if (context.hasErrors()) {
                failed[0]++;
                addErrors(errors, context.errors(), spec.maxErrors(), truncated);
            } else {
                counters[1]++;
                if (spec.rowSink() != null) {
                    spec.rowSink().accept(value);
                }
            }
        });

        return new ImportResult(counters[0], counters[1], failed[0], errors, truncated[0]);
    }

    private static void addErrors(List<RowError> target, List<RowError> rowErrors,
                                  int maxErrors, boolean[] truncated) {
        for (RowError error : rowErrors) {
            if (maxErrors > 0 && target.size() >= maxErrors) {
                truncated[0] = true;
                return;
            }
            target.add(error);
        }
    }

    private static String message(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
