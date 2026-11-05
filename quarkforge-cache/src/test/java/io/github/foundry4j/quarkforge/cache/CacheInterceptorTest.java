package io.github.foundry4j.quarkforge.cache;

import io.github.foundry4j.quarkforge.cache.annotation.Cacheable;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// `@Cacheable` 注解增强的端到端测试（需要 Arc 拦截器运行时）。
@QuarkusTest
class CacheInterceptorTest {

    @Inject
    LocalCacheService cache;

    @Inject
    SlowService service;

    @BeforeEach
    void tearDown() {
        cache.evict("qf:cache:user:42");       // namespace="user" + key="#p0" → user:42
        cache.evict("qf:cache:slow:1");
        cache.evict("qf:cache:nulls:7");
    }

    // ---------- 命中缓存 ----------

    @Test
    void cachesReturnValueOnFirstCall() {
        service.reset();
        assertEquals("hello", service.findUser(42));
        assertEquals("hello", service.findUser(42));
        // 只执行了一次 loader
        assertEquals(1, service.invocations());
    }

    @Test
    void evictRemovesCachedValue() {
        service.reset();
        service.findUser(42);
        cache.evict("qf:cache:user:42");
        service.findUser(42);
        assertEquals(2, service.invocations());
    }

    // ---------- null value 缓存 ----------

    @Test
    void cachesNullReturnWithShortTtl() {
        service.reset();
        assertNull(service.nullableFind(7));
        assertNull(service.nullableFind(7));
        // 第二次走哨兵值，不会触发 loader
        assertEquals(1, service.invocations());
    }

    // ---------- single-flight ----------

    @Test
    void concurrentCallsShareOneLoader() throws Exception {
        service.reset();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    start.await(1, TimeUnit.SECONDS);
                    assertEquals("slow", service.slowFind(1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        if (!done.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("并发测试超时");
        }
        // 只有一个线程拿到注册权调用 loader
        assertEquals(1, service.invocations());
    }

    // ---------- 服务 bean ----------

    @ApplicationScoped
    static class SlowService {

        private final AtomicInteger invocations = new AtomicInteger();

        @Cacheable(key = "#p0", namespace = "user", ttl = "PT10M")
        public String findUser(long id) {
            invocations.incrementAndGet();
            return "hello";
        }

        @Cacheable(key = "#p0", namespace = "slow", ttl = "PT10M")
        public String slowFind(long id) {
            invocations.incrementAndGet();
            try {
                Thread.sleep(60);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "slow";
        }

        @Cacheable(key = "#p0", namespace = "nulls", ttl = "PT5M", cacheNull = true)
        public String nullableFind(long id) {
            invocations.incrementAndGet();
            return null;
        }

        public int invocations() {
            return invocations.get();
        }

        public void reset() {
            invocations.set(0);
        }
    }
}
