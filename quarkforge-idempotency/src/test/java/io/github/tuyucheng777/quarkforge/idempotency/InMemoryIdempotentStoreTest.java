package io.github.tuyucheng777.quarkforge.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/// ## InMemoryIdempotentStoreTest
/// 内存幂等存储的行为验证：占用、完成、失败释放、过期。
class InMemoryIdempotentStoreTest {

    private final InMemoryIdempotentStore store = new InMemoryIdempotentStore();

    @Test
    void firstAcquireReturnsNullAndCreatesProcessingEntry() {
        IdempotentEntry entry = store.tryAcquire("k1", 600);
        assertNull(entry);
        // 再次占用应返回 PROCESSING
        IdempotentEntry second = store.tryAcquire("k1", 600);
        assertNotNull(second);
        assertEquals(IdempotentEntry.State.PROCESSING, second.state());
    }

    @Test
    void doneStoresPayloadForReplay() {
        store.tryAcquire("k2", 600);
        byte[] payload = "hello".getBytes();
        store.markDone("k2", payload, 600);

        IdempotentEntry entry = store.tryAcquire("k2", 600);
        assertNotNull(entry);
        assertEquals(IdempotentEntry.State.DONE, entry.state());
        assertSame(payload, entry.payload());
    }

    @Test
    void failedReleasesTheKey() {
        store.tryAcquire("k3", 600);
        store.markFailed("k3");

        IdempotentEntry entry = store.tryAcquire("k3", 600);
        assertNull(entry);
    }

    @Test
    void expiredEntryCanBeReAcquired() throws Exception {
        store.tryAcquire("k4", 1);
        Thread.sleep(1100);
        IdempotentEntry entry = store.tryAcquire("k4", 600);
        assertNull(entry);
    }
}
