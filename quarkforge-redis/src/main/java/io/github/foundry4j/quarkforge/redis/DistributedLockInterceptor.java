package io.github.foundry4j.quarkforge.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.time.Duration;

/// ## DistributedLockInterceptor
/// `@DistributedLock` 的拦截器：
///
/// 1. 解析锁键模板（方法级注解优先于类级）；
/// 2. 通过 [RedisLockService] 抢锁，`waitSeconds > 0` 时按固定间隔重试直至超时；
/// 3. 抢锁失败抛出 `409 CONFLICT`，业务侧/前端按"操作进行中"提示即可；
/// 4. 业务方法执行完毕（含异常）在 `finally` 中持令牌解锁。
@DistributedLock
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 10)
public class DistributedLockInterceptor {

    /// 等待模式下的重试间隔（毫秒）。
    private static final long RETRY_INTERVAL_MS = 50;

    @Inject
    RedisLockService lockService;

    @Inject
    ObjectMapper objectMapper;

    @AroundInvoke
    Object around(InvocationContext ctx) throws Exception {
        DistributedLock rule = resolve(ctx);
        if (rule == null) {
            return ctx.proceed();
        }

        String key = rule.prefix()
                + LockKeyResolver.resolve(rule.key(), ctx.getMethod(), ctx.getParameters(), objectMapper);
        Duration lease = Duration.ofSeconds(Math.max(1, rule.leaseSeconds()));

        String token = tryAcquire(key, lease, rule.waitSeconds());
        if (token == null) {
            throw new BizException(CommonErrorCode.CONFLICT);
        }
        try {
            return ctx.proceed();
        } finally {
            lockService.unlock(key, token);
        }
    }

    /// 方法级注解优先，其次类级。
    private static DistributedLock resolve(InvocationContext ctx) {
        DistributedLock rule = ctx.getMethod().getAnnotation(DistributedLock.class);
        return rule != null ? rule : ctx.getMethod().getDeclaringClass().getAnnotation(DistributedLock.class);
    }

    /// 单次抢锁；`waitSeconds > 0` 时在期限内按固定间隔重试。
    private String tryAcquire(String key, Duration lease, long waitSeconds) {
        String token = lockService.tryLock(key, lease);
        long deadline = System.nanoTime() + waitSeconds * 1_000_000_000L;
        while (token == null && System.nanoTime() < deadline) {
            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            token = lockService.tryLock(key, lease);
        }
        return token;
    }
}
