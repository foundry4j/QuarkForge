package io.github.tuyucheng777.quarkforge.redis;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.github.tuyucheng777.quarkforge.cache.CacheService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class RedisTwoLevelCacheTest {

    @InjectMock
    RedisCacheService redis;

    @Inject
    CacheService cacheService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(redis);
    }

    @Test
    void redisHitIsPromotedToLocalCache() {
        when(redis.get("user:1", String.class)).thenReturn("alice");

        assertEquals("alice", cacheService.get("user:1", String.class, Duration.ofMinutes(5)));
        assertEquals("alice", cacheService.get("user:1", String.class, Duration.ofMinutes(5)));

        // 第二次读取命中本地缓存，不再访问 Redis
        verify(redis, times(1)).get("user:1", String.class);
    }

    @Test
    void putWritesBothLayers() {
        cacheService.put("user:2", "bob", Duration.ofMinutes(5));

        assertEquals("bob", cacheService.get("user:2", String.class, Duration.ofMinutes(5)));
        verify(redis, times(1)).set("user:2", "bob", Duration.ofMinutes(5));
        verify(redis, times(0)).get("user:2", String.class);
    }

    @Test
    void getOrLoadHitsRedisBeforeLoader() {
        AtomicInteger loads = new AtomicInteger();
        when(redis.get("user:3", String.class)).thenReturn("carol");

        String value = cacheService.getOrLoad("user:3", String.class, Duration.ofMinutes(5), () -> {
            loads.incrementAndGet();
            return "loaded";
        });

        assertEquals("carol", value);
        assertEquals(0, loads.get());
    }

    @Test
    void evictRemovesFromBothLayers() {
        cacheService.put("user:4", "dave", Duration.ofMinutes(5));
        cacheService.evict("user:4");

        verify(redis, times(1)).evict("user:4");
        assertNull(cacheService.get("user:4", String.class, Duration.ofMinutes(5)));
    }

    @Test
    void putIgnoresNullValue() {
        cacheService.put("user:5", null, Duration.ofMinutes(5));

        verify(redis, times(0)).set(anyString(), any(), any());
    }
}
