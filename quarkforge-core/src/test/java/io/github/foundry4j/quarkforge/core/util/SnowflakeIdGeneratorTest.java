package io.github.foundry4j.quarkforge.core.util;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdGeneratorTest {

    @Test
    void generatesUniqueIdsWithinSameMillisecond() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);

        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            ids.add(generator.nextId());
        }

        assertEquals(5000, ids.size());
    }

    @Test
    void idsAreMonotonicallyIncreasing() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(7);
        long previous = generator.nextId();

        for (int i = 0; i < 1000; i++) {
            long current = generator.nextId();
            assertTrue(current > previous);
            previous = current;
        }
    }

    @Test
    void rejectsOutOfRangeWorkerId() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1024));
    }
}
