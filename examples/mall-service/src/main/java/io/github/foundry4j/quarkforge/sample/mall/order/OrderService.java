package io.github.foundry4j.quarkforge.sample.mall.order;

import io.github.foundry4j.quarkforge.audit.OperationLog;
import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.idempotency.Idempotent;
import io.github.foundry4j.quarkforge.sample.mall.order.dto.CreateOrderRequest;
import io.github.foundry4j.quarkforge.sample.mall.order.dto.OrderItemRequest;
import io.github.foundry4j.quarkforge.sample.mall.order.dto.OrderResponse;
import io.github.foundry4j.quarkforge.sample.mall.product.Product;
import io.github.foundry4j.quarkforge.sample.mall.product.ProductService;
import io.github.foundry4j.quarkforge.sample.mall.product.ProductStatus;
import io.github.foundry4j.quarkforge.sample.mall.user.UserService;
import io.github.foundry4j.quarkforge.security.AuthContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/// ## OrderService
/// 订单领域服务：下单（幂等 + 库存扣减）、查询与取消（库存回滚）。
@ApplicationScoped
public class OrderService {

    @Inject
    OrderRepository orderRepository;

    @Inject
    OrderItemRepository itemRepository;

    @Inject
    ProductService productService;

    @Inject
    OrderNumberGenerator orderNumberGenerator;

    @Inject
    AuthContext authContext;

    /// 创建订单。相同用户 + 相同 `requestNo` 在 10 分钟内重复提交时直接返回首次结果。
    @Idempotent(key = "order:#userId:#request.requestNo", ttlSeconds = 600)
    @OperationLog(module = "order", action = "create", description = "创建订单")
    @Transactional
    public OrderResponse create(Long userId, CreateOrderRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest line : request.items()) {
            if (line.quantity() <= 0) {
                throw new BizException(CommonErrorCode.PARAM_ERROR, "商品数量必须大于 0");
            }
            Product product = productService.requireEntity(line.productId());
            if (product.status != ProductStatus.ON_SALE) {
                throw new BizException(CommonErrorCode.PARAM_ERROR, "商品已下架: " + product.name);
            }
            productService.deductStock(product.id, line.quantity());

            OrderItem item = new OrderItem();
            item.productId = product.id;
            item.productName = product.name;
            item.price = product.price;
            item.quantity = line.quantity();
            item.subtotal = product.price.multiply(BigDecimal.valueOf(line.quantity()));
            total = total.add(item.subtotal);
            items.add(item);
        }

        Order order = new Order();
        order.orderNo = orderNumberGenerator.next();
        order.requestNo = request.requestNo().trim();
        order.userId = userId;
        order.totalAmount = total;
        orderRepository.persist(order);
        orderRepository.flush();

        for (OrderItem item : items) {
            item.orderId = order.id;
            itemRepository.persist(item);
        }
        return OrderResponse.from(order, items);
    }

    /// 查询订单详情，仅本人或管理员可见。
    public OrderResponse requireById(Long id) {
        Order order = orderRepository.requireById(id);
        requireAccess(order.userId);
        return OrderResponse.from(order, itemRepository.listByOrderId(id));
    }

    /// 分页查询当前用户的订单。
    public PageResult<OrderResponse> listMine(int page, int size) {
        Long userId = currentUserId();
        return orderRepository.pageByUser(userId, page, size)
                .map(order -> OrderResponse.from(order, itemRepository.listByOrderId(order.id)));
    }

    /// 取消订单并恢复库存，仅 `CREATED` 状态可取消。
    @OperationLog(module = "order", action = "cancel", description = "取消订单")
    @Transactional
    public OrderResponse cancel(Long id) {
        Order order = orderRepository.requireById(id);
        requireAccess(order.userId);
        if (order.status != OrderStatus.CREATED) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "当前订单状态不可取消");
        }
        List<OrderItem> items = itemRepository.listByOrderId(id);
        items.forEach(item -> productService.restoreStock(item.productId, item.quantity));
        order.status = OrderStatus.CANCELLED;
        return OrderResponse.from(order, items);
    }

    /// 订单归属校验：本人或管理员可访问。
    private void requireAccess(Long ownerId) {
        if (ownerId != null && ownerId == currentUserId()) {
            return;
        }
        if (authContext.hasRole(UserService.ROLE_ADMIN)) {
            return;
        }
        throw new BizException(CommonErrorCode.FORBIDDEN);
    }

    private long currentUserId() {
        return Long.parseLong(authContext.userId());
    }
}
