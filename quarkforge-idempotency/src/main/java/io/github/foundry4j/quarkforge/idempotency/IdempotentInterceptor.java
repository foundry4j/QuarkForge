package io.github.foundry4j.quarkforge.idempotency;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/// ## IdempotentInterceptor
/// [Idempotent] 注解的拦截器实现。
///
/// 执行流程：
/// 1. 通过 [IdempotentKeyResolver] 生成幂等键；
/// 2. `tryAcquire` 占用键：
///    - 返回 `null`（首次）→ 执行业务方法，成功后序列化返回值并 `markDone`，失败则 `markFailed`；
///    - 返回 `DONE` → 反序列化缓存结果直接返回，不再执行业务方法；
///    - 返回 `PROCESSING` → 抛出 `BizException(CONFLICT)`，由 web 层映射为 HTTP 409。
///
/// 优先级设在限流拦截器（`PLATFORM_BEFORE + 200`）之后，确保先限流再判幂等。
@Idempotent
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 300)
public class IdempotentInterceptor {

    @Inject
    IdempotentStore store;

    @Inject
    IdempotentKeyResolver keyResolver;

    @Inject
    ObjectMapper objectMapper;

    @AroundInvoke
    Object aroundInvoke(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        Idempotent config = resolveBinding(context, method);
        String key = keyResolver.resolve(context, config, method);

        IdempotentEntry entry = store.tryAcquire(key, config.ttlSeconds());
        if (entry == null) {
            return proceedAndCache(context, method, key, config.ttlSeconds());
        }
        if (entry.state() == IdempotentEntry.State.DONE) {
            return deserialize(entry.payload(), method);
        }
        throw new BizException(CommonErrorCode.CONFLICT);
    }

    private Object proceedAndCache(InvocationContext context, Method method, String key, long ttlSeconds)
            throws Exception {
        Object result;
        try {
            result = context.proceed();
        } catch (Exception e) {
            store.markFailed(key);
            throw e;
        }
        byte[] payload = serialize(result, method);
        store.markDone(key, payload, ttlSeconds);
        return result;
    }

    private byte[] serialize(Object result, Method method) throws Exception {
        if (isVoid(method.getReturnType())) {
            return new byte[0];
        }
        return objectMapper.writeValueAsBytes(result);
    }

    private Object deserialize(byte[] payload, Method method) throws Exception {
        if (isVoid(method.getReturnType())) {
            return null;
        }
        if (payload == null || payload.length == 0) {
            return null;
        }
        Type genericType = method.getGenericReturnType();
        JavaType javaType = objectMapper.getTypeFactory().constructType(genericType);
        return objectMapper.readValue(payload, javaType);
    }

    private boolean isVoid(Class<?> type) {
        return type == void.class || type == Void.class;
    }

    private Idempotent resolveBinding(InvocationContext context, Method method) {
        Idempotent binding = context.getInterceptorBinding(Idempotent.class);
        if (binding != null) {
            return binding;
        }
        Idempotent onMethod = method.getAnnotation(Idempotent.class);
        if (onMethod != null) {
            return onMethod;
        }
        return method.getDeclaringClass().getAnnotation(Idempotent.class);
    }
}
