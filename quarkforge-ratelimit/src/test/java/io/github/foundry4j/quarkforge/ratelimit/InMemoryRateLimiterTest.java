package io.github.foundry4j.quarkforge.ratelimit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimiterTest {

    @Test
    void allowsRequestsWithinPermits() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter();

        assertTrue(limiter.tryAcquire("a", 3, 60));
        assertTrue(limiter.tryAcquire("a", 3, 60));
        assertTrue(limiter.tryAcquire("a", 3, 60));
        assertFalse(limiter.tryAcquire("a", 3, 60));
    }

    @Test
    void differentKeysHaveIndependentQuotas() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter();

        assertTrue(limiter.tryAcquire("a", 1, 60));
        assertFalse(limiter.tryAcquire("a", 1, 60));
        assertTrue(limiter.tryAcquire("b", 1, 60));
    }

    @Test
    void quotaRecoversAfterWindowExpires() throws InterruptedException {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter();

        assertTrue(limiter.tryAcquire("a", 1, 1));
        assertFalse(limiter.tryAcquire("a", 1, 1));

        Thread.sleep(1100);

        assertTrue(limiter.tryAcquire("a", 1, 1));
    }
}
