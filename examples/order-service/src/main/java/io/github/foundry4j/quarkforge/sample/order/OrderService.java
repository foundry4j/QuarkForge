package io.github.foundry4j.quarkforge.sample.order;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.rpc.RpcErrors;
import io.github.foundry4j.quarkforge.sample.order.dto.CreateOrderRequest;
import io.github.foundry4j.quarkforge.sample.user.grpc.GetUserRequest;
import io.github.foundry4j.quarkforge.sample.user.grpc.UserReply;
import io.github.foundry4j.quarkforge.sample.user.grpc.UserServiceGrpc;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/// ## OrderService
/// 订单领域服务。下单前通过 gRPC 调用 user-service 校验用户是否存在且状态正常，
/// 校验通过后持久化订单。
@ApplicationScoped
public class OrderService {

    @Inject
    OrderRepository repository;

    /// 指向 user-service 的 gRPC 客户端，名称与 application.properties 中
    /// `quarkus.grpc.clients.user.*` 配置对应。
    @GrpcClient("user")
    UserServiceGrpc.UserServiceBlockingStub userClient;

    /// 创建订单。
    /// 1. 通过 gRPC 查询用户；不存在或被禁用时拒绝下单
    /// 2. 持久化订单
    @Transactional
    public Order create(CreateOrderRequest request) {
        UserReply user = fetchUser(request.userId());
        if ("DISABLED".equals(user.getStatus())) {
            throw new BizException(CommonErrorCode.FORBIDDEN, "用户已被禁用，无法下单");
        }

        Order order = new Order();
        order.userId = request.userId();
        order.productName = request.productName();
        order.quantity = request.quantity();
        order.amount = request.amount();
        repository.persist(order);
        return order;
    }

    /// 按 ID 查询订单。
    public Order requireById(Long id) {
        return repository.requireById(id);
    }

    /// 分页查询某用户的订单。
    public PageResult<Order> listByUser(Long userId, int page, int size) {
        return repository.listByUser(userId, page, size);
    }

    /// 调用 user-service 按 ID 查询用户，gRPC 异常统一还原为业务异常。
    private UserReply fetchUser(Long userId) {
        try {
            return userClient.getUser(GetUserRequest.newBuilder().setId(userId).build());
        } catch (StatusRuntimeException e) {
            throw RpcErrors.toBizException(e);
        }
    }
}
