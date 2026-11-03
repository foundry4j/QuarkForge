package io.github.foundry4j.quarkforge.rpc.server;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.github.foundry4j.quarkforge.rpc.RpcTracing;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logmanager.MDC;

/// ## RpcServerTraceInterceptor
/// 服务端链路拦截器：读取 `x-trace-id` 元数据写入日志 MDC，
/// 使 RPC 调用日志与 HTTP 入口日志共享同一 traceId。
///
/// 配置项：
///
/// | 配置 | 默认值 | 说明 |
/// |------|--------|------|
/// | `quarkforge.rpc.trace.enabled` | `true` | traceId 透传开关 |
@GlobalInterceptor
@ApplicationScoped
public class RpcServerTraceInterceptor implements ServerInterceptor {

    @ConfigProperty(name = "quarkforge.rpc.trace.enabled", defaultValue = "true")
    boolean enabled;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
            Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
        String traceId = enabled ? headers.get(RpcTracing.TRACE_ID_KEY) : null;
        if (traceId == null || traceId.isBlank()) {
            return listener;
        }
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {

            @Override
            public void onMessage(ReqT message) {
                withTrace(traceId, () -> super.onMessage(message));
            }

            @Override
            public void onHalfClose() {
                withTrace(traceId, super::onHalfClose);
            }

            private void withTrace(String traceId, Runnable action) {
                String previous = MDC.get(RpcTracing.TRACE_ID_MDC_KEY);
                MDC.put(RpcTracing.TRACE_ID_MDC_KEY, traceId);
                try {
                    action.run();
                } finally {
                    if (previous == null) {
                        MDC.remove(RpcTracing.TRACE_ID_MDC_KEY);
                    } else {
                        MDC.put(RpcTracing.TRACE_ID_MDC_KEY, previous);
                    }
                }
            }
        };
    }
}
