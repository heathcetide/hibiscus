package hibiscus.cetide.app.component.cache.index;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.Map;

public class CacheIndex<K> {
    private final AtomicReferenceArray<IndexBucket<K>>[] segments;
    private final int segmentMask;
    private final int bucketMask;
    private final Function<K, Long> hashFunction;
    
    @SuppressWarnings("unchecked")
    public CacheIndex(int segmentCount, int bucketCount, Function<K, Long> hashFunction) {
        // 确保是2的幂
        segmentCount = nextPowerOfTwo(segmentCount);
        bucketCount = nextPowerOfTwo(bucketCount);
        
        this.segments = new AtomicReferenceArray[segmentCount];
        this.segmentMask = segmentCount - 1;
        this.bucketMask = bucketCount - 1;
        this.hashFunction = hashFunction;
        
        for (int i = 0; i < segmentCount; i++) {
            segments[i] = new AtomicReferenceArray<>(bucketCount);
            for (int j = 0; j < bucketCount; j++) {
                segments[i].set(j, new IndexBucket<>());
            }
        }
    }
    
    public void add(K key) {
        long hashValue = hashFunction.apply(key);
        int segmentIndex = (int) (Math.abs(hashValue) & (segments.length - 1));
        int bucketIndex = (int) (Math.abs(hashValue) & bucketMask);
        segments[segmentIndex].get(bucketIndex).add(key);
    }
    
    public boolean contains(K key) {
        long hash = hashFunction.apply(key);
        int segmentIndex = (int) (hash >>> 32) & segmentMask;
        int bucketIndex = (int) hash & bucketMask;
        
        return segments[segmentIndex].get(bucketIndex).contains(key);
    }
    
    public void reorganize(Map<K, Double> scores) {
        // 创建新的分段
        AtomicReferenceArray<IndexBucket<K>>[] newSegments = createNewSegments();
        
        // 根据得分重新分配键
        scores.forEach((key, score) -> {
            int optimalSegment = calculateOptimalSegment(score);
            long hashValue = hashFunction.apply(key);
            int bucketIndex = (int) (Math.abs(hashValue) & bucketMask);
            newSegments[optimalSegment].get(bucketIndex).add(key);
        });
        
        // 原子性地替换旧分段
        for (int i = 0; i < segments.length; i++) {
            segments[i] = newSegments[i];
        }
    }
    
    private int calculateOptimalSegment(double score) {
        // 根据得分计算最优分段
        // 高分键放在前面的分段以提高访问速度
        return (int) (score * segmentMask);
    }
    
    @SuppressWarnings("unchecked")
    private AtomicReferenceArray<IndexBucket<K>>[] createNewSegments() {
        AtomicReferenceArray<IndexBucket<K>>[] newSegments = 
            new AtomicReferenceArray[segments.length];
        
        for (int i = 0; i < segments.length; i++) {
            newSegments[i] = new AtomicReferenceArray<>(segments[i].length());
            for (int j = 0; j < segments[i].length(); j++) {
                newSegments[i].set(j, new IndexBucket<>());
            }
        }
        
        return newSegments;
    }
    
    private static class IndexBucket<K> {
        private static final int BUCKET_SIZE = 8;
        private final AtomicReferenceArray<K> entries;
        private final AtomicInteger size;
        
        public IndexBucket() {
            this.entries = new AtomicReferenceArray<>(BUCKET_SIZE);
            this.size = new AtomicInteger(0);
        }
        
        public void add(K key) {
            int index = size.get();
            if (index < BUCKET_SIZE) {
                if (size.compareAndSet(index, index + 1)) {
                    entries.set(index, key);
                }
            }
        }
        
        public boolean contains(K key) {
            int count = size.get();
            for (int i = 0; i < count; i++) {
                K entry = entries.get(i);
                if (entry != null && entry.equals(key)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    private static int nextPowerOfTwo(int n) {
        n--;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : n + 1;
    }
    
    private int calculateSegment(K key) {
        long hashValue = key.hashCode();
        return (int) (Math.abs(hashValue) & (segments.length - 1));
    }
} 