package io.github.foundry4j.quarkforge.excel;

import java.util.ArrayList;
import java.util.List;

/// ## RowContext
/// 单行导入上下文，校验器通过它登记错误，而不会中断整个导入流程。
///
/// ```java
/// (user, ctx) -> {
///     if (user.name().isBlank()) {
///         ctx.reject("name", ctx.row().cell("name"), "名称不能为空");
///     }
/// }
/// ```
public final class RowContext {

    private final RowData row;
    private final List<RowError> errors = new ArrayList<>();

    public RowContext(RowData row) {
        if (row == null) {
            throw new IllegalArgumentException("row 不能为空");
        }
        this.row = row;
    }

    public RowData row() {
        return row;
    }

    public int rowNumber() {
        return row.rowNumber();
    }

    /// 登记一条字段级错误。
    public void reject(String field, String value, String message) {
        errors.add(new RowError(row.rowNumber(), field, value, message, row.values()));
    }

    /// 登记一条整行错误。
    public void reject(String message) {
        reject(null, null, message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /// 当前行已登记的错误（快照）。
    public List<RowError> errors() {
        return List.copyOf(errors);
    }
}
