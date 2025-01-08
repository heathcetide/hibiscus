package hibiscus.cetide.app.component.cache.stats;

import java.util.concurrent.atomic.LongAdder;

public class CacheStats {
    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();
    private final LongAdder evictions = new LongAdder();
    private final LongAdder expirations = new LongAdder();

    public void recordHit() { hits.increment(); }
    public void recordMiss() { misses.increment(); }
    public void recordEviction() { evictions.increment(); }
    public void recordExpiration() { expirations.increment(); }

    public long getHitCount() { return hits.sum(); }
    public long getMissCount() { return misses.sum(); }
    public long getEvictionCount() { return evictions.sum(); }
    public long getExpirationCount() { return expirations.sum(); }
    
    public double getHitRate() {
        long totalRequests = hits.sum() + misses.sum();
        return totalRequests == 0 ? 0.0 : (double) hits.sum() / totalRequests;
    }
} 