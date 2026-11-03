package io.github.foundry4j.quarkforge.web.api;

import jakarta.ws.rs.QueryParam;

/// ## PageQuery
/// 分页查询参数封装，配合 `@BeanParam` 使用：
///
/// ```java
/// @GET
/// public Result<PageResult<User>> list(@BeanParam PageQuery pageQuery) { ... }
/// ```
///
/// 参数会自动归一化：`page` 最小为 1，`size` 限制在 [1, 100]。
public class PageQuery {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    /// 页码，从 1 开始。
    @QueryParam("page")
    public int page = DEFAULT_PAGE;

    /// 每页大小。
    @QueryParam("size")
    public int size = DEFAULT_SIZE;

    /// 归一化后的页码（≥ 1）。
    public int page() {
        return Math.max(page, 1);
    }

    /// 归一化后的每页大小（[1, 100]）。
    public int size() {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /// 数据库查询偏移量。
    public int offset() {
        return (page() - 1) * size();
    }
}
