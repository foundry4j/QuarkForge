package io.github.foundry4j.quarkforge.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.foundry4j.quarkforge.core.context.HttpRequestContext;
import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// ## OperationLogInterceptor
/// [OperationLog] 注解的拦截器实现：在方法执行前后采集操作信息并落库。
///
/// - 操作人复用 [AuditActorProvider]（`ActorContext` > 当前登录用户 > `system`）；
/// - 参数以 JSON 形式记录，可通过 `recordArgs = false` 关闭；
/// - 成功与失败都记录，失败记录使用独立事务写入，业务回滚不影响日志；
/// - web 场景下额外记录请求方法、路径、客户端 IP、User-Agent 与 traceId。
///
/// 优先级设在限流、幂等拦截器之前，被限流或幂等重放的请求同样会被记录。
@OperationLog
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class OperationLogInterceptor {

    private static final int MAX_ARGS_LENGTH = 8_000;
    private static final int MAX_ERROR_LENGTH = 2_000;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    AuditActorProvider actorProvider;

    @Inject
    OperationLogWriter writer;

    @Inject
    @ConfigProperty(name = "quarkforge.audit.operation-log.enabled", defaultValue = "true")
    boolean enabled;

    @AroundInvoke
    Object aroundInvoke(InvocationContext context) throws Exception {
        if (!enabled) {
            return context.proceed();
        }
        OperationLog config = resolveBinding(context);
        Instant startTime = Instant.now();
        long startNanos = System.nanoTime();
        boolean success = false;
        Throwable failure = null;
        try {
            Object result = context.proceed();
            success = true;
            return result;
        } catch (Exception | Error e) {
            failure = e;
            throw e;
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            record(config, context, startTime, durationMs, success, failure);
        }
    }

    private void record(OperationLog config, InvocationContext context, Instant startTime,
                        long durationMs, boolean success, Throwable failure) {
        OperationLogEntry entry = new OperationLogEntry();
        Method method = context.getMethod();
        entry.module = truncate(config.module().isBlank()
                ? method.getDeclaringClass().getSimpleName() : config.module(), 128);
        entry.action = truncate(config.action().isBlank() ? method.getName() : config.action(), 64);
        entry.description = truncate(config.description(), 512);
        entry.actor = truncate(actorProvider.currentActor(), 64);
        entry.method = truncate(method.getDeclaringClass().getSimpleName() + "#" + method.getName(), 256);
        entry.args = config.recordArgs() ? truncate(serializeArgs(context), MAX_ARGS_LENGTH) : null;
        entry.success = success;
        entry.errorMessage = failure == null ? null : truncate(errorMessage(failure), MAX_ERROR_LENGTH);
        entry.durationMs = durationMs;
        entry.occurredAt = startTime;

        HttpRequestContext.current().ifPresent(info -> {
            entry.requestMethod = truncate(info.method(), 16);
            entry.requestPath = truncate(info.path(), 512);
            entry.ip = truncate(info.ip(), 64);
            entry.userAgent = truncate(info.userAgent(), 512);
            entry.traceId = truncate(info.traceId(), 64);
        });

        try {
            writer.write(entry);
        } catch (Exception e) {
            // 日志写入失败不影响业务
            Log.warnf(e, "Failed to persist operation log for %s", entry.method);
        }
    }

    private String serializeArgs(InvocationContext context) {
        Object[] parameters = context.getParameters();
        if (parameters == null || parameters.length == 0) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (Exception e) {
            return Stream.of(parameters)
                    .map(String::valueOf)
                    .collect(Collectors.joining(",", "[", "]"));
        }
    }

    private String errorMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getName()
                : failure.getClass().getName() + ": " + message;
    }

    private OperationLog resolveBinding(InvocationContext context) {
        OperationLog binding = context.getInterceptorBinding(OperationLog.class);
        if (binding != null) {
            return binding;
        }
        OperationLog onMethod = context.getMethod().getAnnotation(OperationLog.class);
        if (onMethod != null) {
            return onMethod;
        }
        return context.getMethod().getDeclaringClass().getAnnotation(OperationLog.class);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
