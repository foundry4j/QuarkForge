package io.github.foundry4j.quarkforge.sample.mall.order;

import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.sample.mall.order.dto.CreateOrderRequest;
import io.github.foundry4j.quarkforge.sample.mall.order.dto.OrderResponse;
import io.github.foundry4j.quarkforge.sample.mall.user.UserService;
import io.github.foundry4j.quarkforge.security.AuthContext;
import io.github.foundry4j.quarkforge.web.api.PageQuery;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/// ## OrderResource
/// 订单接口，需要登录后访问。
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "订单")
public class OrderResource {

    @Inject
    OrderService orderService;

    @Inject
    AuthContext authContext;

    /// 创建订单，`requestNo` 相同的重复请求由幂等组件拦截。
    @POST
    @RolesAllowed({UserService.ROLE_BUYER, UserService.ROLE_ADMIN})
    public OrderResponse create(@Valid CreateOrderRequest request) {
        return orderService.create(Long.parseLong(authContext.userId()), request);
    }

    /// 当前用户的订单分页。
    @GET
    @Path("/mine")
    @RolesAllowed({UserService.ROLE_BUYER, UserService.ROLE_ADMIN})
    public PageResult<OrderResponse> mine(@BeanParam PageQuery pageQuery) {
        return orderService.listMine(pageQuery.page(), pageQuery.size());
    }

    /// 订单详情，仅本人或管理员可见。
    @GET
    @Path("/{id}")
    @RolesAllowed({UserService.ROLE_BUYER, UserService.ROLE_ADMIN})
    public OrderResponse get(@PathParam("id") Long id) {
        return orderService.requireById(id);
    }

    /// 取消订单并恢复库存。
    @POST
    @Path("/{id}/cancel")
    @RolesAllowed({UserService.ROLE_BUYER, UserService.ROLE_ADMIN})
    public OrderResponse cancel(@PathParam("id") Long id) {
        return orderService.cancel(id);
    }
}
