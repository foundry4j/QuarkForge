package io.github.foundry4j.quarkforge.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class SchedulerServiceTest {

    @Test
    void schedulerBeanIsResolvable() {
        SchedulerService scheduler = Arc.container().instance(SchedulerService.class).get();
        assertNotNull(scheduler, "SchedulerService default bean should be installed");
    }

    // 通过测试类静态字段在 lambda 与测试方法之间通信。
    // JobRunr 把静态方法引用（TestClass::staticMethod）序列化为
    // 类名 + 方法名，后台 runner 通过反射直接调用，不需要 ArC 容器。
    private static volatile CountDownLatch latch;
    private static final AtomicInteger counter = new AtomicInteger();

    @Test
    void enqueueRunsJobEventually() throws Exception {
        latch = new CountDownLatch(1);
        counter.set(0);

        SchedulerService scheduler = Arc.container().instance(SchedulerService.class).get();
        scheduler.enqueue(SchedulerServiceTest::runOnce);

        assertTrue(latch.await(30, TimeUnit.SECONDS),
                "enqueued job should execute within timeout");
        assertTrue(counter.get() >= 1);
    }

    @Test
    void scheduleAtRunsJobAtFutureTime() throws Exception {
        latch = new CountDownLatch(1);

        SchedulerService scheduler = Arc.container().instance(SchedulerService.class).get();
        scheduler.scheduleAt(Instant.now().plus(Duration.ofSeconds(2)),
                SchedulerServiceTest::runOnce);

        assertTrue(latch.await(30, TimeUnit.SECONDS),
                "scheduled-at job should execute at or after requested time");
    }

    /// 被 JobRunr 后台反射调用的静态方法。必须 public 才能通过
    /// [java.lang.reflect.Method.invoke] 访问。
    @SuppressWarnings("unused")
    public static void runOnce() {
        counter.incrementAndGet();
        CountDownLatch l = latch;
        if (l != null) {
            l.countDown();
        }
    }
}
