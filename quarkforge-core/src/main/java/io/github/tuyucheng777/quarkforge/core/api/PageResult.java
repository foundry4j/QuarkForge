package io.github.tuyucheng777.quarkforge.core.api;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/// ## PageResult
/// 统一分页结果结构。
///
/// @param records 当前页数据
/// @param total   总记录数
/// @param page    当前页码（从 1 开始）
/// @param size    每页大小
public record PageResult<T>(List<T> records, long total, int page, int size) implements Serializable {

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        return new PageResult<>(records, total, page, size);
    }

    /// 空页，保留分页参数。
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), 0, page, size);
    }

    /// 总页数。
    public long pages() {
        return size <= 0 ? 0 : (total + size - 1) / size;
    }

    /// 是否有下一页。
    public boolean hasNext() {
        return page < pages();
    }

    /// 对当前页数据做类型转换，分页信息保持不变。
    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(records.stream().map(mapper).toList(), total, page, size);
    }
}
