package hibiscus.cetide.app.component.cache.monitor;

import hibiscus.cetide.app.component.cache.HibiscusCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class CacheMonitorService<K, V> {
    private static final Logger logger = LoggerFactory.getLogger(CacheMonitorService.class);
    
    private final HibiscusCache<K, V> cache;
    private final ScheduledExecutorService scheduler;
    private final long monitoringInterval;
    private final TimeUnit timeUnit;
    
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    
    public CacheMonitorService(HibiscusCache<K, V> cache, long interval, TimeUnit unit) {
        this.cache = cache;
        this.monitoringInterval = interval;
        this.timeUnit = unit;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    public void start() {
        scheduler.scheduleAtFixedRate(this::collectMetrics, 0, monitoringInterval, timeUnit);
        logger.info("Cache monitoring started with interval: {} {}", monitoringInterval, timeUnit);
    }
    
    public void shutdown() {
        scheduler.shutdown();
        logger.info("Cache monitoring stopped");
    }
    
    private void collectMetrics() {
        try {
            // 从缓存获取统计信息
            hitCount.set(cache.getStats().getHitCount());
            missCount.set(cache.getStats().getMissCount());
            evictionCount.set(cache.getStats().getEvictionCount());
            
            // 记录日志
//            logger.debug("Cache metrics collected - Size: {}, Hit Rate: {}%, Evictions: {}",
//                    getSize(), String.format("%.2f",getHitRate() * 100), getEvictionCount());
        } catch (Exception e) {
            logger.error("Error collecting cache metrics", e);
        }
    }
    
    // 获取缓存大小
    public int getSize() {
        return cache.size();
    }
    
    // 获取命中次数
    public long getHitCount() {
        return hitCount.get();
    }
    
    // 获取未命中次数
    public long getMissCount() {
        return missCount.get();
    }
    
    // 获取命中率
    public double getHitRate() {
        long hits = hitCount.get();
        long total = hits + missCount.get();
        return total == 0 ? 0.0 : (double) hits / total;
    }
    
    // 获取驱逐次数
    public long getEvictionCount() {
        return evictionCount.get();
    }
}