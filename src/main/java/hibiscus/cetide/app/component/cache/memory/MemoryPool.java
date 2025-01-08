package hibiscus.cetide.app.component.cache.memory;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryPool {
    private static final int[] DEFAULT_SIZES = {
        64, 128, 256, 512, 1024, 2048, 4096, 8192
    };
    
    private final AtomicReferenceArray<byte[]>[] pools;
    private final int[] sizes;
    private final AtomicInteger[] counters;
    
    public MemoryPool() {
        this(DEFAULT_SIZES, 1024);
    }
    
    @SuppressWarnings("unchecked")
    public MemoryPool(int[] sizes, int poolSize) {
        this.sizes = sizes;
        this.pools = new AtomicReferenceArray[sizes.length];
        this.counters = new AtomicInteger[sizes.length];
        
        for (int i = 0; i < sizes.length; i++) {
            pools[i] = new AtomicReferenceArray<>(poolSize);
            counters[i] = new AtomicInteger();
        }
    }
    
    public byte[] acquire(int size) {
        int poolIndex = findPoolIndex(size);
        if (poolIndex < 0) {
            return new byte[size];
        }
        
        AtomicReferenceArray<byte[]> pool = pools[poolIndex];
        AtomicInteger counter = counters[poolIndex];
        
        for (int i = 0; i < 3; i++) { // 重试3次
            int index = counter.get() % pool.length();
            byte[] buffer = pool.getAndSet(index, null);
            if (buffer != null) {
                return buffer;
            }
            counter.incrementAndGet();
        }
        
        return new byte[sizes[poolIndex]];
    }
    
    public void release(byte[] buffer) {
        int poolIndex = findPoolIndex(buffer.length);
        if (poolIndex < 0) return;
        
        AtomicReferenceArray<byte[]> pool = pools[poolIndex];
        int count = counters[poolIndex].get();
        
        if (count < pool.length()) {
            if (pool.compareAndSet(count, null, buffer)) {
                counters[poolIndex].incrementAndGet();
            }
        }
    }
    
    private int findPoolIndex(int size) {
        for (int i = 0; i < sizes.length; i++) {
            if (size <= sizes[i]) return i;
        }
        return -1;
    }
} 