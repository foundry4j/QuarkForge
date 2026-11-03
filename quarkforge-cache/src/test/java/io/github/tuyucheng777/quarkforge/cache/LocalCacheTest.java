package io.github.tuyucheng777.quarkforge.cache;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@QuarkusTest
class LocalCacheTest {

    @Inject
    CacheService cacheService;

    @Test
    void storesAndReadsValues() {
        cacheService.put("user:1", "alice", Duration.ofMinutes(5));

        assertEquals("alice", cacheService.get("user:1", String.class, Duration.ofMinutes(5)));
        assertNull(cacheService.get("user:2", String.class, Duration.ofMinutes(5)));
    }

    @Test
    void getOrLoadOnlyCallsLoaderOnMiss() {
        AtomicInteger loads = new AtomicInteger();
        String first = cacheService.getOrLoad("user:3", String.class, Duration.ofMinutes(5), () -> {
            loads.incrementAndGet();
            return "bob";
        });
        String second = cacheService.getOrLoad("user:3", String.class, Duration.ofMinutes(5), () -> {
            loads.incrementAndGet();
            return "bob";
        });

        assertEquals("bob", first);
        assertEquals("bob", second);
        assertEquals(1, loads.get());
    }

    @Test
    void evictRemovesEntry() {
        cacheService.put("user:4", "carol", Duration.ofMinutes(5));
        cacheService.evict("user:4");

        assertNull(cacheService.get("user:4", String.class, Duration.ofMinutes(5)));
    }

    @Test
    void entryExpiresAfterTtl() throws InterruptedException {
        cacheService.put("short", "value", Duration.ofMillis(50));
        Thread.sleep(150);

        assertNull(cacheService.get("short", String.class, Duration.ofMillis(50)));
    }

    @Test
    void cachesSameInstance() {
        Object value = new Object();
        cacheService.put("object", value, Duration.ofMinutes(5));

        assertSame(value, cacheService.get("object", Object.class, Duration.ofMinutes(5)));
    }
}
