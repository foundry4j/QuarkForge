package io.github.foundry4j.quarkforge.excel;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/// ## RowReadOptions
/// 流式读取选项。
///
/// @param sheetIndex     Excel 工作表下标（0 起始），CSV 忽略
/// @param headerRowIndex 表头所在行的 0 起始下标；`-1` 表示没有表头
/// @param skipEmptyRows  是否跳过整行空白的数据行
/// @param charset        CSV 编码，默认 UTF-8
public record RowReadOptions(int sheetIndex, int headerRowIndex, boolean skipEmptyRows, Charset charset) {

    public RowReadOptions {
        if (sheetIndex < 0) {
            throw new IllegalArgumentException("sheetIndex 不能为负数");
        }
        if (headerRowIndex < -1) {
            throw new IllegalArgumentException("headerRowIndex 最小为 -1");
        }
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }
    }

    /// 默认：第 1 个工作表、首行为表头、跳过空行、UTF-8。
    public static RowReadOptions defaults() {
        return new RowReadOptions(0, 0, true, StandardCharsets.UTF_8);
    }
}
