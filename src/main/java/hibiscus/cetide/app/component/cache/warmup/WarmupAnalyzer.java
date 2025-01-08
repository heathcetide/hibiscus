package hibiscus.cetide.app.component.cache.warmup;

import java.util.concurrent.atomic.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

public class WarmupAnalyzer {
    private final AtomicLong totalWarmups;
    private final AtomicLong totalLatency;
    private final AtomicInteger errorCount;
    private final Queue<WarmupEvent> recentEvents;
    private final int maxEvents = 1000;
    private final double scoreThreshold = 0.5;
    
    private static class WarmupEvent {
        final int batchSize;
        final long timestamp;
        final long latency;
        
        public WarmupEvent(int batchSize, long latency) {
            this.batchSize = batchSize;
            this.timestamp = System.nanoTime();
            this.latency = latency;
        }
    }
    
    public WarmupAnalyzer() {
        this.totalWarmups = new AtomicLong();
        this.totalLatency = new AtomicLong();
        this.errorCount = new AtomicInteger();
        this.recentEvents = new ConcurrentLinkedQueue<>();
    }
    
    public void recordWarmupBatch(int batchSize) {
        long startTime = System.nanoTime();
        totalWarmups.addAndGet(batchSize);
        long latency = System.nanoTime() - startTime;
        totalLatency.addAndGet(latency);
        
        addEvent(new WarmupEvent(batchSize, latency));
    }
    
    private void addEvent(WarmupEvent event) {
        recentEvents.offer(event);
        while (recentEvents.size() > maxEvents) {
            recentEvents.poll();
        }
    }
    
    public void recordError() {
        errorCount.incrementAndGet();
    }
    
    public double getScoreThreshold() {
        return scoreThreshold;
    }
    
    public WarmupAnalysis analyze() {
        long total = totalWarmups.get();
        if (total == 0) {
            return new WarmupAnalysis(0, 0, 0, 0);
        }
        
        double avgLatency = totalLatency.get() / (double) total;
        double hitRate = calculateHitRate();
        double throughput = calculateThroughput();
        int errors = errorCount.get();
        
        return new WarmupAnalysis(avgLatency, hitRate, throughput, errors);
    }
    
    private double calculateHitRate() {
        if (recentEvents.isEmpty()) return 1.0;
        
        int hits = 0;
        int total = 0;
        for (WarmupEvent event : recentEvents) {
            total += event.batchSize;
            if (event.latency < 1_000_000) { // 1ms
                hits += event.batchSize;
            }
        }
        
        return total == 0 ? 1.0 : hits / (double) total;
    }
    
    private double calculateThroughput() {
        if (recentEvents.isEmpty()) return 0.0;
        
        long now = System.nanoTime();
        long oldestTime = Long.MAX_VALUE;
        int totalItems = 0;
        
        for (WarmupEvent event : recentEvents) {
            oldestTime = Math.min(oldestTime, event.timestamp);
            totalItems += event.batchSize;
        }
        
        double durationSeconds = (now - oldestTime) / 1_000_000_000.0;
        return durationSeconds == 0 ? 0.0 : totalItems / durationSeconds;
    }
} 