package io.github.foundry4j.quarkforge.sample.user;

import io.github.foundry4j.quarkforge.sample.user.grpc.GetUserRequest;
import io.github.foundry4j.quarkforge.sample.user.grpc.UserReply;
import io.github.foundry4j.quarkforge.sample.user.grpc.UserServiceGrpc;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/// ## UserGrpcService
/// 用户 gRPC 服务端实现，将请求转发给领域服务 [UserService]。
///
/// 其它微服务可通过 gRPC 按 ID 查询用户，避免直接暴露 HTTP 接口，
/// 同时复用框架统一的错误映射与 traceId 透传。
@GrpcService
@Singleton
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    @Inject
    UserService userService;

    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserReply> responseObserver) {
        User user = userService.requireById(request.getId());
        responseObserver.onNext(UserReply.newBuilder()
                .setId(user.id)
                .setUsername(user.username)
                .setDisplayName(user.displayName)
                .setStatus(user.status.name())
                .build());
        responseObserver.onCompleted();
    }
}
