package io.github.foundry4j.quarkforge.idempotency;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// ## IdempotentInterceptorTest
/// 通过一个测试 Bean 验证 [Idempotent] 拦截器的缓存回放、并发冲突、失败释放与 TTL 过期行为。
@QuarkusTest
class IdempotentInterceptorTest {

    @Inject
    IdempotentService service;

    @Test
    void repeatedCallReturnsCachedResultWithoutReExecution() {
        service.reset();
        String first = service.create("ORD-1");
        String second = service.create("ORD-1");

        assertEquals(first, second);
        assertEquals(1, service.getCalls());
    }

    @Test
    void differentKeysExecuteIndependently() {
        service.reset();
        service.create("ORD-A");
        service.create("ORD-B");

        assertEquals(2, service.getCalls());
    }

    @Test
    void concurrentSameKeyReturnsConflict() throws Exception {
        service.reset();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch firstStarted = new CountDownLatch(1);

        Future<String> first = pool.submit(() -> service.slow("C", firstStarted));
        firstStarted.await(2, TimeUnit.SECONDS);
        Future<String> second = pool.submit(() -> service.slow("C", new CountDownLatch(0)));

        ExecutionException ex = assertThrows(ExecutionException.class, () -> second.get(2, TimeUnit.SECONDS));
        assertInstanceOf(BizException.class, ex.getCause());
        assertEquals(CommonErrorCode.CONFLICT.code(), ((BizException) ex.getCause()).code());

        first.get(2, TimeUnit.SECONDS);
        pool.shutdownNow();
    }

    @Test
    void failedExecutionReleasesKeyForRetry() {
        service.reset();
        assertThrows(IllegalStateException.class, () -> service.failing("F"));
        // 失败后应允许再次执行
        assertThrows(IllegalStateException.class, () -> service.failing("F"));
        assertEquals(2, service.getCalls());
    }

    @Test
    void expiredEntryAllowsReExecution() throws Exception {
        service.reset();
        service.shortLived("E");
        Thread.sleep(1100);
        service.shortLived("E");
        assertEquals(2, service.getCalls());
    }

    /// 测试用 Bean：每个方法都标注了 [Idempotent]。
    @ApplicationScoped
    public static class IdempotentService {

        private volatile int calls;

        public int getCalls() {
            return calls;
        }

        public void reset() {
            calls = 0;
        }

        @Idempotent(key = "create:#orderNo", ttlSeconds = 600)
        public String create(String orderNo) {
            calls++;
            return "ok-" + orderNo + "-" + calls;
        }

        @Idempotent(key = "slow:#id", ttlSeconds = 600)
        public String slow(String id, CountDownLatch started) throws InterruptedException {
            started.countDown();
            calls++;
            Thread.sleep(800);
            return "slow-" + id;
        }

        @Idempotent(key = "fail:#id", ttlSeconds = 600)
        public String failing(String id) {
            calls++;
            throw new IllegalStateException("boom");
        }

        @Idempotent(key = "short:#id", ttlSeconds = 1)
        public String shortLived(String id) {
            calls++;
            return "short-" + id;
        }
    }
}
