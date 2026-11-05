package io.github.foundry4j.quarkforge.cache;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicIntegerArray;

/// ## SimpleBloomFilter
/// 零依赖布隆过滤器，基于 MurmurHash3 x64_128 两个哈希值派生出多组探测位置。
///
/// 目标插入量、目标误判率在构造时固定；实例不可扩展，支持 `put` / `mightContain`。
///
/// 注：布隆过滤器只保证「可能存在」，不保证「一定存在」，
/// 因此业务侧仍需要空值缓存来处理 Bloom 的误判。
///
/// ```java
/// // 预期插入 10000 条，允许 1% 误判
/// SimpleBloomFilter bloom = new SimpleBloomFilter(10_000, 0.01);
/// bloom.put("key");
/// bloom.mightContain("key");   // true
/// bloom.mightContain("never"); // 大概率 false，但不能 100% 保证
/// ```
public final class SimpleBloomFilter {

    private final AtomicIntegerArray bits;
    private final int bitSize;
    private final int numHashFunctions;

    public SimpleBloomFilter(int expectedInsertions, double falsePositiveRate) {
        if (expectedInsertions <= 0) {
            expectedInsertions = 1000;
        }
        if (falsePositiveRate <= 0 || falsePositiveRate >= 1) {
            falsePositiveRate = 0.01;
        }
        // m = -n * ln(p) / (ln 2)^2
        long m = (long) Math.ceil(-expectedInsertions * Math.log(falsePositiveRate)
                / (Math.log(2) * Math.log(2)));
        this.bitSize = Math.max(64, (int) Math.min(m, Integer.MAX_VALUE));
        // k = m / n * ln 2
        this.numHashFunctions = Math.clamp((int) Math.round((double) bitSize / expectedInsertions * Math.log(2)), 1, 31);
        this.bits = new AtomicIntegerArray((bitSize + 31) / 32);
    }

    /// 把键加入 Bloom。
    public void put(CharSequence key) {
        if (key == null) {
            return;
        }
        byte[] bytes = key.toString().getBytes(StandardCharsets.UTF_8);
        long[] hashes = murmur128(bytes);
        long h1 = hashes[0];
        long h2 = hashes[1];
        for (int i = 0; i < numHashFunctions; i++) {
            long combined = h1 + i * h2;
            int index = Math.abs((int) (combined % bitSize));
            bits.set(index >>> 5, bits.get(index >>> 5) | (1 << (index & 31)));
        }
    }

    /// 可能包含（也可能是误判）。
    public boolean mightContain(CharSequence key) {
        if (key == null) {
            return false;
        }
        byte[] bytes = key.toString().getBytes(StandardCharsets.UTF_8);
        long[] hashes = murmur128(bytes);
        long h1 = hashes[0];
        long h2 = hashes[1];
        for (int i = 0; i < numHashFunctions; i++) {
            long combined = h1 + i * h2;
            int index = Math.abs((int) (combined % bitSize));
            if ((bits.get(index >>> 5) & (1 << (index & 31))) == 0) {
                return false;
            }
        }
        return true;
    }

    public int bitSize() {
        return bitSize;
    }

    public int hashFunctionCount() {
        return numHashFunctions;
    }

    // ---------- Murmur3 x64_128 ----------

    private static long[] murmur128(byte[] key) {
        long h1 = 0x9e3779b97f4a7c15L;
        long h2 = 0x9e3779b97f4a7c15L;
        long c1 = 0x87c37b91114253d5L;
        long c2 = 0x4cf5ad432745937fL;

        int len = key.length;
        int blockEnd = (len / 16) * 16;

        for (int i = 0; i < blockEnd; i += 16) {
            long k1 = getLittleEndian(key, i);
            long k2 = getLittleEndian(key, i + 8);

            k1 *= c1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= c2;
            h1 ^= k1;

            h1 = Long.rotateLeft(h1, 27);
            h1 += h2;
            h1 = h1 * 5 + 0x52dce729;

            k2 *= c2;
            k2 = Long.rotateLeft(k2, 33);
            k2 *= c1;
            h2 ^= k2;

            h2 = Long.rotateLeft(h2, 31);
            h2 += h1;
            h2 = h2 * 5 + 0x38495ab5;
        }

        long k1 = 0;
        long k2 = 0;
        int tail = blockEnd;
        int tailLen = len & 15;

        switch (tailLen) {
            case 15:
                k2 ^= ((long) (key[tail + 14] & 0xff)) << 48;
            case 14:
                k2 ^= ((long) (key[tail + 13] & 0xff)) << 40;
            case 13:
                k2 ^= ((long) (key[tail + 12] & 0xff)) << 32;
            case 12:
                k2 ^= ((long) (key[tail + 11] & 0xff)) << 24;
            case 11:
                k2 ^= ((long) (key[tail + 10] & 0xff)) << 16;
            case 10:
                k2 ^= ((long) (key[tail + 9] & 0xff)) << 8;
            case 9:
                k2 ^= key[tail + 8] & 0xff;
                k2 *= c2;
                k2 = Long.rotateLeft(k2, 33);
                k2 *= c1;
                h2 ^= k2;
            case 8:
                k1 ^= ((long) (key[tail + 7] & 0xff)) << 56;
            case 7:
                k1 ^= ((long) (key[tail + 6] & 0xff)) << 48;
            case 6:
                k1 ^= ((long) (key[tail + 5] & 0xff)) << 40;
            case 5:
                k1 ^= ((long) (key[tail + 4] & 0xff)) << 32;
            case 4:
                k1 ^= ((long) (key[tail + 3] & 0xff)) << 24;
            case 3:
                k1 ^= ((long) (key[tail + 2] & 0xff)) << 16;
            case 2:
                k1 ^= ((long) (key[tail + 1] & 0xff)) << 8;
            case 1:
                k1 ^= key[tail] & 0xff;
                k1 *= c1;
                k1 = Long.rotateLeft(k1, 31);
                k1 *= c2;
                h1 ^= k1;
            default:
                break;
        }

        h1 ^= len;
        h2 ^= len;

        h1 += h2;
        h2 += h1;

        h1 = fmix64(h1);
        h2 = fmix64(h2);

        h1 += h2;
        h2 += h1;

        return new long[]{h1, h2};
    }

    private static long fmix64(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }

    private static long getLittleEndian(byte[] data, int offset) {
        return ((long) (data[offset] & 0xff))
                | (((long) (data[offset + 1] & 0xff)) << 8)
                | (((long) (data[offset + 2] & 0xff)) << 16)
                | (((long) (data[offset + 3] & 0xff)) << 24)
                | (((long) (data[offset + 4] & 0xff)) << 32)
                | (((long) (data[offset + 5] & 0xff)) << 40)
                | (((long) (data[offset + 6] & 0xff)) << 48)
                | (((long) (data[offset + 7] & 0xff)) << 56);
    }

    @Override
    public String toString() {
        return "SimpleBloomFilter{size=" + bitSize + ",k=" + numHashFunctions + "}";
    }

    // 防止 Arrays 未使用的警告
    @SuppressWarnings("unused")
    private static byte[] copy(byte[] src) {
        return Arrays.copyOf(src, src.length);
    }
}
