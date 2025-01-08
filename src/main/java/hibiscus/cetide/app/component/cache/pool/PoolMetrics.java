package hibiscus.cetide.app.component.cache.pool;

import java.util.concurrent.atomic.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PoolMetrics {
    private final ConcurrentHashMap<String, PoolStats> poolStats;
    private final AtomicLong totalAcquisitions;
    private final AtomicLong totalAcquisitionTime;
    private final AtomicInteger missCount;
    
    private static class PoolStats {
        final AtomicInteger size = new AtomicInteger();
        final AtomicInteger borrowed = new AtomicInteger();
        final AtomicLong hitCount = new AtomicLong();
        final AtomicLong missCount = new AtomicLong();
        
        public void update(int size, int borrowed) {
            this.size.set(size);
            this.borrowed.set(borrowed);
            if (borrowed > 0) {
                hitCount.incrementAndGet();
            } else {
                missCount.incrementAndGet();
            }
        }
    }
    
    public PoolMetrics() {
        this.poolStats = new ConcurrentHashMap<>();
        this.totalAcquisitions = new AtomicLong();
        this.totalAcquisitionTime = new AtomicLong();
        this.missCount = new AtomicInteger();
    }
    
    public void recordPoolStats(String type, int size, int borrowed) {
        poolStats.computeIfAbsent(type, k -> new PoolStats())
                 .update(size, borrowed);
    }
    
    public void recordAcquisition(long time) {
        totalAcquisitions.incrementAndGet();
        totalAcquisitionTime.addAndGet(time);
    }
    
    public void recordMiss() {
        missCount.incrementAndGet();
    }
    
    public double getAverageAcquisitionTime() {
        long count = totalAcquisitions.get();
        return count == 0 ? 0.0 : 
            totalAcquisitionTime.get() / (double) count;
    }
    
    public double getHitRatio() {
        long total = totalAcquisitions.get();
        int misses = missCount.get();
        return total == 0 ? 1.0 : 
            (total - misses) / (double) total;
    }
    
    public Map<String, PoolStats> getPoolStats() {
        return new ConcurrentHashMap<>(poolStats);
    }
    
    public String getReport() {
        StringBuilder report = new StringBuilder();
        report.append("Pool Metrics Report:\n")
              .append("Average Acquisition Time: ")
              .append(String.format("%.2f ns", getAverageAcquisitionTime()))
              .append("\n")
              .append("Hit Ratio: ")
              .append(String.format("%.2f%%", getHitRatio() * 100))
              .append("\n\nPool Stats:\n");
        
        poolStats.forEach((type, stats) -> {
            report.append(type)
                  .append(": size=").append(stats.size.get())
                  .append(", borrowed=").append(stats.borrowed.get())
                  .append(", hits=").append(stats.hitCount.get())
                  .append(", misses=").append(stats.missCount.get())
                  .append("\n");
        });
        
        return report.toString();
    }
} 