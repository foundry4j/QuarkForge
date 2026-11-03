package io.github.foundry4j.quarkforge.rpc.echo;

import io.grpc.stub.StreamObserver;
import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.rpc.RpcTracing;
import io.github.foundry4j.quarkforge.rpc.grpc.EchoGrpc;
import io.github.foundry4j.quarkforge.rpc.grpc.EchoRequest;
import io.github.foundry4j.quarkforge.rpc.grpc.EchoResponse;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Singleton;

/// ## EchoService
/// 内置连通性诊断服务：原样返回消息并回显服务端看到的 traceId，
/// 可用于部署后探测 gRPC 链路、拦截器与超时配置是否生效。
///
/// 特殊消息（诊断错误映射用）：
///
/// - `throw:biz` 抛出 `BizException(NOT_FOUND)`
/// - `throw:boom` 抛出未受检异常
@GrpcService
@Singleton
public class EchoService extends EchoGrpc.EchoImplBase {

    @Override
    public void ping(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
        String message = request.getMessage();
        switch (message) {
            case "throw:biz" -> throw new BizException(CommonErrorCode.NOT_FOUND, "echo resource not found");
            case "throw:boom" -> throw new IllegalStateException("boom");
            default -> {
                String traceId = RpcTracing.currentTraceId();
                responseObserver.onNext(EchoResponse.newBuilder()
                        .setMessage(message)
                        .setTraceId(traceId == null ? "" : traceId)
                        .build());
                responseObserver.onCompleted();
            }
        }
    }
}
