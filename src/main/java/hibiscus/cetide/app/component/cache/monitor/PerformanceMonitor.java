package hibiscus.cetide.app.component.cache.monitor;

import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceMonitor {
    private final ConcurrentHashMap<String, MetricCollector> metrics;
    private final ScheduledExecutorService scheduler;
    private final int windowSize;
    private final TimeUnit windowUnit;
    
    public static class MetricCollector {
        private final LongAdder count = new LongAdder();
        private final LongAdder totalTime = new LongAdder();
        private final AtomicLong maxTime = new AtomicLong();
        private final SlidingTimeWindow timeWindow;
        
        public MetricCollector(int windowSize, TimeUnit unit) {
            this.timeWindow = new SlidingTimeWindow(windowSize, unit);
        }
        
        public void record(long startTime) {
            long duration = System.nanoTime() - startTime;
            count.increment();
            totalTime.add(duration);
            updateMax(duration);
            timeWindow.record(duration);
        }
        
        private void updateMax(long duration) {
            long current;
            do {
                current = maxTime.get();
                if (duration <= current) break;
            } while (!maxTime.compareAndSet(current, duration));
        }
        
        public double getAverageLatency() {
            long count = this.count.sum();
            return count == 0 ? 0 : totalTime.sum() / (double) count;
        }
        
        public double getRecentThroughput() {
            return timeWindow.getThroughput();
        }
    }
    
    private static class SlidingTimeWindow {
        private final long[] buckets;
        private final long[] counts;
        private final long bucketDuration;
        private int currentBucket;
        
        public SlidingTimeWindow(int windowSize, TimeUnit unit) {
            this.buckets = new long[windowSize];
            this.counts = new long[windowSize];
            this.bucketDuration = unit.toNanos(1);
        }
        
        public synchronized void record(long duration) {
            long now = System.nanoTime();
            int bucket = (int)((now / bucketDuration) % buckets.length);
            
            if (bucket != currentBucket) {
                // 清理旧桶
                for (int i = currentBucket + 1; i != bucket; i = (i + 1) % buckets.length) {
                    buckets[i] = 0;
                    counts[i] = 0;
                }
                currentBucket = bucket;
            }
            
            buckets[bucket] += duration;
            counts[bucket]++;
        }
        
        public double getThroughput() {
            long totalCount = 0;
            for (long count : counts) {
                totalCount += count;
            }
            return totalCount / (double) buckets.length;
        }
    }
    
    public PerformanceMonitor(int windowSize, TimeUnit windowUnit) {
        this.metrics = new ConcurrentHashMap<>();
        this.windowSize = windowSize;
        this.windowUnit = windowUnit;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        startReporting();
    }
    
    public void recordOperation(String operation, long startTime) {
        metrics.computeIfAbsent(operation, 
            k -> new MetricCollector(windowSize, windowUnit))
            .record(startTime);
    }
    
    private void startReporting() {
        scheduler.scheduleAtFixedRate(() -> {
            StringBuilder report = new StringBuilder("\nPerformance Report:\n");
            metrics.forEach((op, collector) -> {
                report.append(String.format(
                    "%s: avg=%.2fms, tps=%.2f/s\n",
                    op,
                    collector.getAverageLatency() / 1_000_000.0,
                    collector.getRecentThroughput()
                ));
            });
//            System.out.println(report);
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
    
    public double getAverageLatency(String operation) {
        // 实现获取平均延迟的逻辑
        return 0.0;
    }
    
    public double getThroughput(String operation) {
        // 实现获取吞吐量的逻辑
        return 0.0;
    }
} 