package io.github.foundry4j.quarkforge.core.api;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResultTest {

    @Test
    void computesPagesAndHasNext() {
        PageResult<Integer> result = PageResult.of(List.of(1, 2, 3), 25, 2, 10);

        assertEquals(3, result.pages());
        assertTrue(result.hasNext());
    }

    @Test
    void lastPageHasNoNext() {
        PageResult<Integer> result = PageResult.of(List.of(1, 2, 3), 23, 3, 10);

        assertEquals(3, result.pages());
        assertFalse(result.hasNext());
    }

    @Test
    void mapPreservesPagingMetadata() {
        PageResult<Integer> result = PageResult.of(List.of(1, 2), 12, 1, 10);

        PageResult<String> mapped = result.map(String::valueOf);

        assertEquals(List.of("1", "2"), mapped.records());
        assertEquals(12, mapped.total());
        assertEquals(1, mapped.page());
        assertEquals(10, mapped.size());
    }
}
