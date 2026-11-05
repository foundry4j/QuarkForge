package io.github.foundry4j.quarkforge.excel;

/// 业务校验器；通过 [RowContext#reject] 登记错误。
///
/// 抛出的异常会被导入器捕获并记为该行错误，不会中断整个文件。
@FunctionalInterface
public interface RowValidator<T> {

    void validate(T value, RowContext context) throws Exception;
}
