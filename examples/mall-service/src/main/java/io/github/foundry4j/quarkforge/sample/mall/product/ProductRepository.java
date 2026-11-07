package io.github.foundry4j.quarkforge.sample.mall.product;

import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.data.BaseRepository;
import jakarta.enterprise.context.ApplicationScoped;

/// ## ProductRepository
/// 商品仓储。
@ApplicationScoped
public class ProductRepository extends BaseRepository<Product> {

    /// 按名称或描述模糊检索，关键字为空时返回全量分页。
    public PageResult<Product> search(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            return pageAll(page, size);
        }
        return page(find("name like ?1 or description like ?1", "%" + keyword.trim() + "%"), page, size);
    }
}
