package io.github.foundry4j.quarkforge.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// [SimpleBloomFilter] 的纯单元测试。
class SimpleBloomFilterTest {

    @Test
    void returnsTrueForInsertedKeys() {
        SimpleBloomFilter bloom = new SimpleBloomFilter(1000, 0.01);
        for (int i = 0; i < 1000; i++) {
            bloom.put("key-" + i);
        }
        for (int i = 0; i < 1000; i++) {
            assertTrue(bloom.mightContain("key-" + i), "key-" + i + " 应该存在");
        }
    }

    @Test
    void allowsSmallFalsePositiveRate() {
        SimpleBloomFilter bloom = new SimpleBloomFilter(10000, 0.01);
        for (int i = 0; i < 10000; i++) {
            bloom.put("k-" + i);
        }
        int fp = 0;
        for (int i = 10000; i < 20000; i++) {
            if (bloom.mightContain("k-" + i)) {
                fp++;
            }
        }
        // 目标 FPP 1%；真实 FPP 应远低于此
        assertTrue(fp <= 300, "误判率过高: " + fp + " / 10000");
    }

    @Test
    void noKeyNeverReturnsTrueForEmptyFilter() {
        SimpleBloomFilter bloom = new SimpleBloomFilter(1000, 0.01);
        assertFalse(bloom.mightContain("never"));
        assertFalse(bloom.mightContain(""));
        assertFalse(bloom.mightContain(null));
    }
}
