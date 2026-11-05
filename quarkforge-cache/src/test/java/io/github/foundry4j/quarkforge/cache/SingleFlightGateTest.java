package io.github.foundry4j.quarkforge.cache;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// [SingleFlightGate] 的多线程单飞测试。
class SingleFlightGateTest {

    @Test
    void onlyFirstCallRegistersNewFuture() {
        SingleFlightGate gate = new SingleFlightGate();
        var f1 = gate.tryRegister("k");
        var f2 = gate.tryRegister("k");
        assertSame(f1.future(), f2.future(), "同一 key 应返回同一个 future");
        assertTrue(f1.iAmRegistrant(), "首次调用应当是注册者");
        assertTrue(!f2.iAmRegistrant(), "第二次调用不应是注册者");
        assertEquals(1, gate.inFlightCount());
    }

    @Test
    void concurrentRequestsShareTheLoader() throws Exception {
        SingleFlightGate gate = new SingleFlightGate();
        AtomicInteger loaderRunCount = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    start.await(1, TimeUnit.SECONDS);
                    SingleFlightGate.Registration reg = gate.tryRegister("k");
                    if (reg.iAmRegistrant()) {
                        Thread.sleep(50);
                        gate.complete("k", "value");
                        loaderRunCount.incrementAndGet();
                    }
                    Object r = reg.future().get(2, TimeUnit.SECONDS);
                    assertEquals("value", r);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        // loaderRunCount 最终应为 1（只有一个线程在 loader 里调用了 complete）
        assertEquals(1, loaderRunCount.get());
        assertEquals(0, gate.inFlightCount());
    }

    @Test
    void failRemovesKeySoNextCallCreatesNewFuture() throws Exception {
        SingleFlightGate gate = new SingleFlightGate();
        var reg1 = gate.tryRegister("k");
        gate.fail("k", new IllegalArgumentException("boom"));
        assertTrue(reg1.future().isCompletedExceptionally());
        assertEquals(0, gate.inFlightCount());

        // 失败后 key 已被移除，下一次会创建新的 future
        var reg2 = gate.tryRegister("k");
        assertNotSame(reg1.future(), reg2.future());
        assertEquals(1, gate.inFlightCount());
    }
}
