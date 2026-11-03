package io.github.foundry4j.quarkforge.rpc.server;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.rpc.RpcErrors;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;

/// ## RpcServerExceptionInterceptor
/// 服务端全局拦截器：将服务实现抛出的异常转换为 gRPC Status。
/// [BizException] 按错误码段位映射并把原始错误码写入 `x-error-code` trailer；
/// 其他异常统一映射为 INTERNAL。
///
/// 注意：仅对「同步抛出」生效（ImplBase 风格或 Mutiny 方法体直接 throw）；
/// Mutiny 接口返回 failed Uni 时由 Quarkus 自行转换，建议直接抛异常。
@GlobalInterceptor
@ApplicationScoped
public class RpcServerExceptionInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
            Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {

            @Override
            public void onMessage(ReqT message) {
                try {
                    super.onMessage(message);
                } catch (Throwable throwable) {
                    close(call, throwable);
                }
            }

            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (Throwable throwable) {
                    close(call, throwable);
                }
            }

            @Override
            public void onReady() {
                try {
                    super.onReady();
                } catch (Throwable throwable) {
                    close(call, throwable);
                }
            }
        };
    }

    private void close(ServerCall<?, ?> call, Throwable throwable) {
        Metadata trailers = new Metadata();
        if (throwable instanceof BizException biz) {
            trailers.put(RpcErrors.ERROR_CODE_KEY, String.valueOf(biz.code()));
        }
        call.close(RpcErrors.toStatus(throwable), trailers);
    }
}
