package io.github.foundry4j.quarkforge.web.filter;

import io.github.foundry4j.quarkforge.core.context.HttpRequestContext;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logmanager.MDC;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import java.util.UUID;

/// ## AccessLogFilter
/// 请求访问日志：为每个请求生成 `traceId` 写入 MDC 与 `X-Trace-Id` 响应头，
/// 结束时输出方法、路径、状态码与耗时。
///
/// 配置项：
///
/// | 配置 | 默认值 | 说明 |
/// |------|--------|------|
/// | `quarkforge.web.access-log.enabled` | `true` | 是否启用访问日志 |
@ApplicationScoped
public class AccessLogFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private static final String START_TIME_PROPERTY = "quarkforge.start-time";

    @ConfigProperty(name = "quarkforge.web.access-log.enabled", defaultValue = "true")
    boolean enabled;

    @ServerRequestFilter
    public void beforeRequest(ContainerRequestContext requestContext) {
        String traceId = resolveTraceId(requestContext);
        HttpRequestContext.set(new HttpRequestContext.Info(
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath(),
                clientIp(requestContext),
                requestContext.getHeaderString("User-Agent"),
                traceId));
        if (!enabled) {
            return;
        }
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        requestContext.setProperty(START_TIME_PROPERTY, System.nanoTime());
    }

    @ServerResponseFilter
    public void afterRequest(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        try {
            if (!enabled) {
                return;
            }
            Object start = requestContext.getProperty(START_TIME_PROPERTY);
            long costMillis = start instanceof Long startNanos
                    ? (System.nanoTime() - startNanos) / 1_000_000
                    : -1;
            String traceId = MDC.get(TRACE_ID_MDC_KEY);
            if (traceId != null) {
                responseContext.getHeaders().putSingle(TRACE_ID_HEADER, traceId);
            }
            Log.infof("%s %s -> %d (%d ms)", requestContext.getMethod(),
                    requestContext.getUriInfo().getPath(), responseContext.getStatus(), costMillis);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
            HttpRequestContext.clear();
        }
    }

    /// 优先沿用调用方（网关/上游服务）传入的 traceId，保证链路连续；否则生成新的。
    private String resolveTraceId(ContainerRequestContext requestContext) {
        String upstream = requestContext.getHeaderString(TRACE_ID_HEADER);
        if (upstream != null && !upstream.isBlank()) {
            return upstream;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /// 客户端 IP：优先 `X-Forwarded-For` 的第一个地址，其次 `X-Real-IP`。
    private String clientIp(ContainerRequestContext requestContext) {
        String forwarded = requestContext.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = requestContext.getHeaderString("X-Real-IP");
        return realIp == null || realIp.isBlank() ? null : realIp.trim();
    }
}
