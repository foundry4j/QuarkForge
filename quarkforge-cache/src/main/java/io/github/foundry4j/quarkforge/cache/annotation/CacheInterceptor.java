package io.github.foundry4j.quarkforge.cache.annotation;

import io.github.foundry4j.quarkforge.cache.CacheService;
import io.github.foundry4j.quarkforge.cache.LocalCacheService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.lang.reflect.Method;
import java.time.Duration;

/// ## CacheInterceptor
/// [Cacheable] 注解拦截器：
///
/// ```text
///           ┌─ Bloom 可能包含 ──► cache.get
///           │                        │ miss
///           │                        ├─ 有 in-flight ─► .get() 等待
///   Cacheable ┤                      └─ 无 in-flight ─► loader() ─► put + bloom
///           │
///           └─ Bloom 一定不包含 ──► 跳过 cache.get，直接 single-flight
/// ```
///
/// 当 CacheService bean 不是 [LocalCacheService]（例如 Redis 两级缓存）时，
/// 自动降级为 `cache.getOrLoad`（保留缓存功能，但不具备 single-flight / bloom 能力）。
@Cacheable
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 250)
public class CacheInterceptor {

    @Inject
    CacheService cache;

    @Inject
    @ConfigProperty(name = "quarkforge.cache.local.default-ttl", defaultValue = "PT5M")
    Duration defaultTtl;

    @AroundInvoke
    Object aroundInvoke(InvocationContext context) throws Exception {
        Cacheable binding = resolveBinding(context);
        String key = CacheKeyResolver.resolve(binding.key(), context.getMethod(), context.getParameters(),
                binding.namespace());
        Duration ttl = parseTtl(binding.ttl());

        Class<?> returnType = context.getMethod().getReturnType();

        // LocalCacheService 提供 bloom + single-flight + null value 三重防护
        if (cache instanceof LocalCacheService local && binding.cacheNull()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object result = local.computeProtected(key, (Class) returnType, ttl,
                    () -> invokeSafe(context));
            return result;
        }

        // 通用降级路径
        Object cached = cache.get(key, returnType, ttl);
        if (cached != null) {
            return cached;
        }
        Object loaded = context.proceed();
        if (loaded != null) {
            cache.put(key, loaded, ttl);
        }
        return loaded;
    }

    private Cacheable resolveBinding(InvocationContext context) {
        Cacheable binding = context.getInterceptorBinding(Cacheable.class);
        if (binding != null) {
            return binding;
        }
        Method m = context.getMethod();
        Cacheable onMethod = m.getAnnotation(Cacheable.class);
        if (onMethod != null) {
            return onMethod;
        }
        return m.getDeclaringClass().getAnnotation(Cacheable.class);
    }

    private Duration parseTtl(String ttl) {
        if (ttl == null || ttl.isBlank()) {
            return defaultTtl;
        }
        try {
            return Duration.parse(ttl.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的 ttl 值: " + ttl, e);
        }
    }

    private Object invokeSafe(InvocationContext ctx) {
        try {
            return ctx.proceed();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
