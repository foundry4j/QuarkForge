package io.github.foundry4j.quarkforge.excel;

/// 行 → 业务对象映射。
///
/// 抛出的异常会被导入器捕获并记为该行错误，不会中断整个文件。
@FunctionalInterface
public interface RowMapper<T> {

    T map(RowData row) throws Exception;
}
