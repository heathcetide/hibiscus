package hibiscus.cetide.app.component.cache.filter;

import java.util.BitSet;
import java.util.function.Function;

public class BloomFilter<T> {
    private final BitSet bitset;
    private final int size;
    private final int hashFunctions;
    private final Function<T, Integer>[] hashers;

    @SuppressWarnings("unchecked")
    public BloomFilter(int expectedElements, double falsePositiveRate) {
        this.size = optimalBitSize(expectedElements, falsePositiveRate);
        this.hashFunctions = optimalHashFunctions(expectedElements, size);
        this.bitset = new BitSet(size);
        this.hashers = new Function[hashFunctions];
        
        for (int i = 0; i < hashFunctions; i++) {
            final int seed = i;
            hashers[i] = item -> {
                int hash = item.hashCode();
                hash = hash * 31 + seed;
                hash = hash ^ (hash >>> 16);
                return Math.abs(hash % size);
            };
        }
    }

    public void add(T item) {
        for (Function<T, Integer> hasher : hashers) {
            bitset.set(hasher.apply(item));
        }
    }

    public boolean mightContain(T item) {
        for (Function<T, Integer> hasher : hashers) {
            if (!bitset.get(hasher.apply(item))) {
                return false;
            }
        }
        return true;
    }

    private static int optimalBitSize(int n, double p) {
        return (int) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    private static int optimalHashFunctions(int n, int m) {
        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }
} 