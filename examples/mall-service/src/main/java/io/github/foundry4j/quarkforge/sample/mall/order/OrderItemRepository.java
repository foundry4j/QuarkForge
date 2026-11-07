package io.github.foundry4j.quarkforge.sample.mall.order;

import io.github.foundry4j.quarkforge.data.BaseRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/// ## OrderItemRepository
/// 订单明细仓储。
@ApplicationScoped
public class OrderItemRepository extends BaseRepository<OrderItem> {

    /// 查询某订单的全部明细。
    public List<OrderItem> listByOrderId(Long orderId) {
        return list("orderId = ?1 order by id", orderId);
    }
}
