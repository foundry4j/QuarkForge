package io.github.foundry4j.quarkforge.sample.order;

import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.data.BaseRepository;
import jakarta.enterprise.context.ApplicationScoped;

/// ## OrderRepository
/// 订单仓储。
@ApplicationScoped
public class OrderRepository extends BaseRepository<Order> {

    /// 按用户 ID 分页查询订单，按创建时间降序。
    public PageResult<Order> listByUser(Long userId, int page, int size) {
        return page(find("userId = ?1 order by createdAt desc", userId), page, size);
    }
}
