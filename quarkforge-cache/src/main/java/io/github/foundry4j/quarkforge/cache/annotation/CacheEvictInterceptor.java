package io.github.foundry4j.quarkforge.cache.annotation;

import io.github.foundry4j.quarkforge.cache.CacheService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.lang.reflect.Method;

/// ## CacheEvictInterceptor
/// [CacheEvict] 注解拦截器：在写方法执行前或成功后失效缓存键。
///
/// `beforeInvocation = true` 适用于「先删后写」场景（例如先失效再写 DB）；
/// 默认是成功返回后失效，避免事务回滚把缓存清了但 DB 没变。
@CacheEvict(key = "", namespace = "", beforeInvocation = false)
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 260)
public class CacheEvictInterceptor {

    @Inject
    CacheService cache;

    @AroundInvoke
    Object aroundInvoke(InvocationContext context) throws Exception {
        CacheEvict[] bindings = resolveBindings(context);
        // beforeInvocation 的先执行失效
        for (CacheEvict ev : bindings) {
            if (ev.beforeInvocation()) {
                cache.evict(resolveKey(ev, context));
            }
        }
        Object result;
        try {
            result = context.proceed();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 成功返回后失效
        for (CacheEvict ev : bindings) {
            if (!ev.beforeInvocation()) {
                cache.evict(resolveKey(ev, context));
            }
        }
        return result;
    }

    private CacheEvict[] resolveBindings(InvocationContext context) {
        CacheEvict.List list = context.getInterceptorBinding(CacheEvict.List.class);
        if (list != null) {
            return list.value();
        }
        CacheEvict binding = context.getInterceptorBinding(CacheEvict.class);
        if (binding != null) {
            return new CacheEvict[]{binding};
        }
        Method m = context.getMethod();
        CacheEvict[] onMethod = m.getAnnotationsByType(CacheEvict.class);
        if (onMethod.length > 0) {
            return onMethod;
        }
        return m.getDeclaringClass().getAnnotationsByType(CacheEvict.class);
    }

    private String resolveKey(CacheEvict ev, InvocationContext ctx) {
        return CacheKeyResolver.resolve(ev.key(), ctx.getMethod(), ctx.getParameters(), ev.namespace());
    }
}
