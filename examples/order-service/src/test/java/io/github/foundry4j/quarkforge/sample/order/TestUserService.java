package io.github.foundry4j.quarkforge.sample.order;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.sample.user.grpc.GetUserRequest;
import io.github.foundry4j.quarkforge.sample.user.grpc.UserReply;
import io.github.foundry4j.quarkforge.sample.user.grpc.UserServiceGrpc;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Singleton;

/// ## TestUserService
/// 测试用的 user-service gRPC 桩实现，随 order-service 测试一起在 in-process
/// gRPC 通道上启动，用于验证订单服务对用户服务的调用与错误映射。
///
/// - id = 1：返回一个 ACTIVE 用户
/// - id = 2：返回一个 DISABLED 用户
/// - 其他：抛出 404 业务异常
@GrpcService
@Singleton
public class TestUserService extends UserServiceGrpc.UserServiceImplBase {

    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserReply> responseObserver) {
        long id = request.getId();
        if (id == 1) {
            responseObserver.onNext(UserReply.newBuilder()
                    .setId(1)
                    .setUsername("alice")
                    .setDisplayName("Alice")
                    .setStatus("ACTIVE")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        if (id == 2) {
            responseObserver.onNext(UserReply.newBuilder()
                    .setId(2)
                    .setUsername("bob")
                    .setDisplayName("Bob")
                    .setStatus("DISABLED")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        throw new BizException(CommonErrorCode.NOT_FOUND, "用户不存在: " + id);
    }
}
