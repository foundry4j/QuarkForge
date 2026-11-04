package io.github.foundry4j.quarkforge.sample.order;

import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.sample.order.dto.CreateOrderRequest;
import io.github.foundry4j.quarkforge.sample.order.dto.OrderResponse;
import io.github.foundry4j.quarkforge.web.api.PageQuery;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/// ## OrderResource
/// 订单接口。创建订单时会经由 gRPC 调用 user-service 校验用户。
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "订单")
public class OrderResource {

    @Inject
    OrderService orderService;

    /// 创建订单。
    @POST
    public OrderResponse create(@Valid CreateOrderRequest request) {
        return OrderResponse.from(orderService.create(request));
    }

    /// 查询订单详情。
    @GET
    @Path("/{id}")
    public OrderResponse get(@PathParam("id") Long id) {
        return OrderResponse.from(orderService.requireById(id));
    }

    /// 按用户分页查询订单。
    @GET
    public PageResult<OrderResponse> list(@QueryParam("userId") Long userId, @BeanParam PageQuery pageQuery) {
        if (userId == null) {
            return PageResult.empty(pageQuery.page(), pageQuery.size());
        }
        return orderService.listByUser(userId, pageQuery.page(), pageQuery.size()).map(OrderResponse::from);
    }
}
