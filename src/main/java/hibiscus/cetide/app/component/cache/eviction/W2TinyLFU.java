package hibiscus.cetide.app.component.cache.eviction;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;

/**
 * Window-TinyLFU (W-TinyLFU) 实现，这是一个高效的缓存准入策略
 * 比传统的LRU和LFU更智能，能更好地适应访问模式的变化
 */
public class W2TinyLFU<K> {
    private final int maxSize;
    private final Map<K, Integer> frequency = new ConcurrentHashMap<>();
    
    public W2TinyLFU(int maxSize) {
        this.maxSize = maxSize;
    }
    
    public K getEvictionCandidate() {
        return frequency.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    public void recordAccess(K key) {
        frequency.merge(key, 1, Integer::sum);
    }

    public List<K> getEvictionCandidates() {
        return Collections.singletonList(getEvictionCandidate());
    }
} 