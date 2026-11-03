package io.github.foundry4j.quarkforge.rpc;

import io.grpc.Metadata;
import org.jboss.logmanager.MDC;

/// ## RpcTracing
/// 链路透传常量：gRPC 元数据头 `x-trace-id` 与日志 MDC `traceId` 的桥接，
/// 与 web 模块 AccessLogFilter 使用同一 MDC 键，保证 HTTP 与 RPC 日志串联。
public final class RpcTracing {

    /// 日志 MDC 中的 traceId 键，与 web 模块保持一致。
    public static final String TRACE_ID_MDC_KEY = "traceId";

    /// gRPC 元数据中的 traceId 键。
    public static final Metadata.Key<String> TRACE_ID_KEY =
            Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    private RpcTracing() {
    }

    /// 当前线程 MDC 中的 traceId，未设置时返回 `null`。
    public static String currentTraceId() {
        return MDC.get(TRACE_ID_MDC_KEY);
    }
}
