package io.github.foundry4j.quarkforge.redis;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// ## DistributedLockInterceptorTest
/// 通过 `@InjectMock RedisLockService` 模拟锁的获取结果，
/// 验证键模板解析、409 语义、等待重试与解锁时机。
@QuarkusTest
class DistributedLockInterceptorTest {

    private static final Duration DEFAULT_LEASE = Duration.ofSeconds(30);

    @InjectMock
    RedisLockService lockService;

    @Inject
    OrderService orders;

    @Inject
    ClassLevelService classLevel;

    @ApplicationScoped
    public static class OrderService {

        @DistributedLock(key = "order:pay:#orderId")
        public String pay(long orderId) {
            return "paid:" + orderId;
        }

        @DistributedLock(key = "#p0")
        public String byIndex(String id) {
            return "idx:" + id;
        }

        @DistributedLock(key = "t:#req.tenantId:o:#req.id")
        public String complex(PayRequest req) {
            return "ok";
        }

        @DistributedLock(key = "#id", waitSeconds = 1)
        public String withWait(String id) {
            return "waited";
        }

        @DistributedLock(key = "boom:#id")
        public String failing(String id) {
            throw new IllegalStateException("biz error");
        }

        @DistributedLock
        public String defaultKey() {
            return "default";
        }
    }

    public record PayRequest(String tenantId, long id) {
    }

    @ApplicationScoped
    @DistributedLock(key = "cls:#id")
    public static class ClassLevelService {

        public String run(String id) {
            return "cls:" + id;
        }
    }

    @BeforeEach
    void stubLock() {
        // 默认抢锁成功，特定用例再覆盖
        when(lockService.tryLock(anyString(), any())).thenReturn("tok");
    }

    // ---------- 键模板解析 ----------

    @Test
    void paramNameExpression() {
        assertEquals("paid:42", orders.pay(42L));
        verify(lockService).tryLock("qf:lock:order:pay:42", DEFAULT_LEASE);
        verify(lockService).unlock("qf:lock:order:pay:42", "tok");
    }

    @Test
    void indexExpression() {
        orders.byIndex("u1");
        verify(lockService).tryLock("qf:lock:u1", DEFAULT_LEASE);
    }

    @Test
    void fieldChainExpression() {
        orders.complex(new PayRequest("t9", 7L));
        verify(lockService).tryLock("qf:lock:t:t9:o:7", DEFAULT_LEASE);
    }

    @Test
    void blankKeyFallsBackToMethodSignature() {
        orders.defaultKey();
        String expected = "qf:lock:" + OrderService.class.getName() + "#defaultKey";
        verify(lockService).tryLock(expected, DEFAULT_LEASE);
    }

    @Test
    void classLevelAnnotationApplies() {
        assertEquals("cls:a", classLevel.run("a"));
        verify(lockService).tryLock("qf:lock:cls:a", DEFAULT_LEASE);
    }

    // ---------- 抢锁失败 ----------

    @Test
    void busyLockThrows409WithoutUnlock() {
        when(lockService.tryLock(anyString(), any())).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> orders.pay(1L));
        assertEquals(CommonErrorCode.CONFLICT.code(), ex.code());
        verify(lockService, never()).unlock(anyString(), anyString());
    }

    // ---------- 等待重试 ----------

    @Test
    void waitsAndRetriesUntilAcquired() {
        when(lockService.tryLock(anyString(), any()))
                .thenReturn(null)
                .thenReturn("tok2");
        assertEquals("waited", orders.withWait("x"));
        verify(lockService, times(2)).tryLock(eq("qf:lock:x"), any());
    }

    @Test
    void waitTimeoutThrows409() {
        when(lockService.tryLock(anyString(), any())).thenReturn(null);
        assertThrows(BizException.class, () -> orders.withWait("x"));
    }

    // ---------- 解锁时机 ----------

    @Test
    void unlocksEvenWhenBusinessThrows() {
        assertThrows(IllegalStateException.class, () -> orders.failing("x"));
        verify(lockService).unlock("qf:lock:boom:x", "tok");
    }
}
