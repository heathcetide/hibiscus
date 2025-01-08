package hibiscus.cetide.app.component.cache.monitor;

import hibiscus.cetide.app.component.cache.cache.CacheEntry;
import hibiscus.cetide.app.component.cache.storage.AdaptiveStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class CacheMonitor<K, V> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheMonitor.class.getName());
    private final ScheduledExecutorService monitorExecutor;
    private final AdaptiveStorage<K, CacheEntry<V>> storage;
    private final AtomicLong hitCount;
    private final AtomicLong missCount;
    private final AtomicLong evictionCount;
    private final AtomicLong loadTime;
    
    public CacheMonitor(AdaptiveStorage<K, CacheEntry<V>> storage) {
        this.storage = storage;
        this.hitCount = new AtomicLong();
        this.missCount = new AtomicLong();
        this.evictionCount = new AtomicLong();
        this.loadTime = new AtomicLong();
        this.monitorExecutor = Executors.newScheduledThreadPool(1);
        
        startMonitoringTask();
    }
    
    private void startMonitoringTask() {
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                logCacheStats();
            } catch (Exception e) {
                LOGGER.warn("Error during cache monitoring", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    public void recordHit() {
        hitCount.incrementAndGet();
    }
    
    public void recordMiss() {
        missCount.incrementAndGet();
    }
    
    public void recordEviction() {
        evictionCount.incrementAndGet();
    }
    
    public void recordLoadTime(long time) {
        loadTime.addAndGet(time);
    }
    
    private void logCacheStats() {
        long hits = hitCount.get();
        long misses = missCount.get();
        long evictions = evictionCount.get();
        long totalLoadTime = loadTime.get();
        long totalRequests = hits + misses;
        
        double hitRate = totalRequests == 0 ? 0.0 : (double) hits / totalRequests;
        double avgLoadTime = hits == 0 ? 0.0 : (double) totalLoadTime / hits;
        
        LOGGER.info(String.format("Cache Stats - Hits: %d, Misses: %d, Evictions: %d, Hit Rate: %.2f%%, Avg Load Time: %.2f ms",
            hits, misses, evictions, hitRate * 100, avgLoadTime / 1_000_000.0));
    }
    
    public void shutdown() {
        monitorExecutor.shutdown();
    }
} 