package hibiscus.cetide.app.component.cache.warmup;


import hibiscus.cetide.app.component.cache.HibiscusCache;

import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.Function;

public class SmartCacheWarmer<K, V> {
    private final ConcurrentHashMap<K, WarmupStats> warmupStats;
    private final ExecutorService warmupExecutor;
    private final HibiscusCache<K, V> cache;
    private final int warmupBatchSize;
    private final WarmupAnalyzer analyzer;
    
    private class WarmupStats implements Comparable<WarmupStats> {
        final K key;
        final AtomicInteger accessCount;
        final AtomicLong lastAccess;
        final DoubleAdder score;
        
        public WarmupStats(K key) {
            this.key = key;
            this.accessCount = new AtomicInteger();
            this.lastAccess = new AtomicLong(System.nanoTime());
            this.score = new DoubleAdder();
        }
        
        public void recordAccess() {
            accessCount.incrementAndGet();
            lastAccess.set(System.nanoTime());
            updateScore();
        }
        
        private void updateScore() {
            double recency = 1.0 / (System.nanoTime() - lastAccess.get() + 1);
            double frequency = accessCount.get();
            score.reset();
            score.add(frequency * recency);
        }
        
        @Override
        public int compareTo(WarmupStats other) {
            return Double.compare(other.score.sum(), score.sum());
        }
    }
    
    public SmartCacheWarmer(HibiscusCache<K, V> cache, int threads, int batchSize) {
        this.cache = cache;
        this.warmupStats = new ConcurrentHashMap<>();
        this.warmupExecutor = Executors.newFixedThreadPool(threads);
        this.warmupBatchSize = batchSize;
        this.analyzer = new WarmupAnalyzer();
        
        startAnalysisTask();
    }
    
    private void startAnalysisTask() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                analyzeAndOptimize();
            } catch (Exception e) {
                // 记录错误但继续运行
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    public void recordAccess(K key) {
        WarmupStats stats = warmupStats.computeIfAbsent(key, k -> new WarmupStats(k));
        stats.recordAccess();
    }
    
    public void warmup(Function<K, V> loader) {
        // 获取热点数据
        PriorityQueue<WarmupStats> hotKeys = new PriorityQueue<>();
        warmupStats.values().forEach(hotKeys::offer);
        
        // 批量预热
        List<K> batch = new ArrayList<>(warmupBatchSize);
        while (!hotKeys.isEmpty() && batch.size() < warmupBatchSize) {
            WarmupStats stats = hotKeys.poll();
            if (stats != null) {
                batch.add(stats.key);
            }
        }
        
        // 并行加载
        CompletableFuture.allOf(
            batch.stream()
                .map(key -> CompletableFuture.runAsync(() -> {
                    try {
                        V value = loader.apply(key);
                        if (value != null) {
                            cache.put(key, value);
                        }
                    } catch (Exception e) {
                        // 记录错误但继续预热其他键
                    }
                }, warmupExecutor))
                .toArray(CompletableFuture[]::new)
        ).join();
    }
    
    private void analyzeAndOptimize() {
        // 实现分析和优化逻辑
    }
    
    public WarmupAnalysis getAnalysis() {
        // 返回预热分析结果
        return new WarmupAnalysis(
            analyzer.getAverageLatency(),
            analyzer.getHitRate(),
            analyzer.getThroughput(),
            analyzer.getErrors()
        );
    }
    
    private class WarmupAnalyzer {
        private final AtomicLong totalLatency = new AtomicLong();
        private final AtomicLong totalOperations = new AtomicLong();
        private final AtomicLong hits = new AtomicLong();
        private final AtomicInteger errorCount = new AtomicInteger();
        
        public double getAverageLatency() {
            long ops = totalOperations.get();
            return ops == 0 ? 0 : totalLatency.get() / (double) ops;
        }
        
        public double getHitRate() {
            long ops = totalOperations.get();
            return ops == 0 ? 0 : hits.get() / (double) ops;
        }
        
        public double getThroughput() {
            // 实现吞吐量计算逻辑
            return 0.0;
        }
        
        public int getErrors() {
            return errorCount.get();
        }
    }
} 