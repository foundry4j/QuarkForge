package io.github.foundry4j.quarkforge.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.arc.DefaultBean;

/// ## InMemoryRateLimiter
/// 基于内存固定窗口的默认限流实现，适用于单机部署。
/// 过期窗口在下次访问时惰性回收。多实例部署请引入 `quarkforge-redis` 模块。
@DefaultBean
@ApplicationScoped
public class InMemoryRateLimiter implements RateLimiter {

    private static final class Window {
        final long startMillis;
        int count;

        Window(long startMillis) {
            this.startMillis = startMillis;
            this.count = 0;
        }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int permits, long windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000;
        Window window = windows.compute(key, (k, current) -> {
            if (current == null || now - current.startMillis >= windowMillis) {
                Window fresh = new Window(now);
                fresh.count = 1;
                return fresh;
            }
            current.count++;
            return current;
        });
        return window.count <= permits;
    }
}
