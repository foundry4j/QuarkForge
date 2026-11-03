package io.github.foundry4j.quarkforge.rpc.client;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.github.foundry4j.quarkforge.rpc.RpcTracing;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/// ## RpcClientTraceInterceptor
/// 客户端全局拦截器，自动应用于 `@GrpcClient` 注入的桩：
/// 把当前 MDC 中的 traceId 写入 `x-trace-id` 元数据向下游透传。
@GlobalInterceptor
@ApplicationScoped
public class RpcClientTraceInterceptor implements ClientInterceptor {

    @ConfigProperty(name = "quarkforge.rpc.trace.enabled", defaultValue = "true")
    boolean enabled;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        String traceId = enabled ? RpcTracing.currentTraceId() : null;
        if (traceId == null || traceId.isBlank()) {
            return next.newCall(method, callOptions);
        }
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                if (headers.get(RpcTracing.TRACE_ID_KEY) == null) {
                    headers.put(RpcTracing.TRACE_ID_KEY, traceId);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
