package io.github.tuyucheng777.quarkforge.data;

import io.github.tuyucheng777.quarkforge.core.api.CommonErrorCode;
import io.github.tuyucheng777.quarkforge.core.api.PageResult;
import io.github.tuyucheng777.quarkforge.core.exception.BizException;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;

/// ## BaseRepository
/// 仓储基类，在 Panache 标准能力之上补充：
///
/// - 分页查询并直接产出统一分页结构 [PageResult]
/// - `requireById`：查不到即抛 `NOT_FOUND` 业务异常，省去判空样板代码
///
/// 用法：
///
/// ```java
/// @ApplicationScoped
/// public class UserRepository extends BaseRepository<User> {
///     public PageResult<User> search(String keyword, int page, int size) {
///         return page(find("name like ?1", "%" + keyword + "%"), page, size);
///     }
/// }
/// ```
///
/// @param <T> 实体类型
public abstract class BaseRepository<T> implements PanacheRepositoryBase<T, Long> {

    /// 对任意 Panache 查询执行分页（`page` 从 1 开始）。
    public PageResult<T> page(PanacheQuery<T> query, int page, int size) {
        int pageIndex = Math.max(page, 1);
        return PageResult.of(query.page(Page.of(pageIndex - 1, size)).list(), query.count(), pageIndex, size);
    }

    /// 全表分页查询。
    public PageResult<T> pageAll(int page, int size) {
        return page(findAll(), page, size);
    }

    /// 按 ID 查询，不存在时抛出 `NOT_FOUND` 业务异常。
    public T requireById(Long id) {
        T entity = findById(id);
        if (entity == null) {
            throw new BizException(CommonErrorCode.NOT_FOUND);
        }
        return entity;
    }
}
