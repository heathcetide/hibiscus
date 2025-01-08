package hibiscus.cetide.app.component.cache.analytics;

import java.util.concurrent.*;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CacheAnalyzer<K, V> {
    private final ConcurrentSkipListMap<Double, Set<K>> hotKeys;
    private final Map<K, AccessPattern> accessPatterns;
    private final ScheduledExecutorService analyzer;
    private final CacheOptimizer optimizer;
    private final AnalyticsConfig config;
    
    public static class AccessPattern {
        public final AtomicLong accessCount = new AtomicLong();
        public final AtomicLong totalLatency = new AtomicLong();
        public final ConcurrentLinkedQueue<Long> recentLatencies = new ConcurrentLinkedQueue<>();
        public final AtomicInteger missCount = new AtomicInteger();
        public volatile long lastAccess = System.nanoTime();
        
        public void recordAccess(long latency, boolean hit) {
            accessCount.incrementAndGet();
            totalLatency.addAndGet(latency);
            recentLatencies.offer(latency);
            if (!hit) {
                missCount.incrementAndGet();
            }
            lastAccess = System.nanoTime();
            
            while (recentLatencies.size() > 100) {
                recentLatencies.poll();
            }
        }
        
        public double getScore() {
            long count = accessCount.get();
            if (count == 0) return 0.0;
            
            double hitRate = 1.0 - (missCount.get() / (double) count);
            double avgLatency = totalLatency.get() / (double) count;
            double recency = 1.0 / (System.nanoTime() - lastAccess + 1);
            
            return hitRate * recency * (1_000_000.0 / avgLatency);
        }
    }
    
    public CacheAnalyzer(AnalyticsConfig config) {
        this.config = config;
        this.hotKeys = new ConcurrentSkipListMap<>();
        this.accessPatterns = new ConcurrentHashMap<>();
        this.optimizer = new CacheOptimizer(config);
        this.analyzer = Executors.newScheduledThreadPool(1);
        
        startAnalysis();
    }
    
    private void startAnalysis() {
        analyzer.scheduleAtFixedRate(() -> {
            try {
                analyze();
                optimize();
            } catch (Exception e) {
                // 记录错误但继续运行
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    public void recordAccess(K key, long latency, boolean hit) {
        AccessPattern pattern = accessPatterns.computeIfAbsent(
            key, k -> new AccessPattern());
        pattern.recordAccess(latency, hit);
    }
    
    private void analyze() {
        // 分析热点数据
        Map<K, AccessPattern> patterns = accessPatterns;
        
        // 生成分析报告
        AnalyticsReport report = new AnalyticsReport();
        report.setHotKeyCount(hotKeys.values().stream()
                               .mapToInt(Set::size)
                               .sum());
        report.setTotalPatterns(patterns.size());
        report.setAverageScore(patterns.values().stream()
                                   .mapToDouble(AccessPattern::getScore)
                                   .average()
                                   .orElse(0.0));
    }
    
    private void optimize() {
        OptimizationResult result = optimizer.optimize(
            hotKeys,
            accessPatterns,
            config
        );
        
        // 应用优化建议
        result.getRecommendations().forEach(recommendation -> {
            switch (recommendation.getType()) {
                case INCREASE_CACHE_SIZE:
                    notifyOptimization(recommendation);
                    break;
                case ADJUST_TTL:
                    notifyOptimization(recommendation);
                    break;
                case PRELOAD_KEYS:
                    notifyOptimization(recommendation);
                    break;
            }
        });
    }
    
    private void generateReport() {
        AnalyticsReport report = new AnalyticsReport();
        report.setHotKeyCount(hotKeys.values().stream()
                                   .mapToInt(Set::size)
                                   .sum());
        report.setTotalPatterns(accessPatterns.size());
        report.setAverageScore(accessPatterns.values().stream()
                                           .mapToDouble(AccessPattern::getScore)
                                           .average()
                                           .orElse(0.0));
        
        notifyReport(report);
    }
    
    private final List<Consumer<OptimizationRecommendation>> optimizationListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<AnalyticsReport>> reportListeners = new CopyOnWriteArrayList<>();
    
    public void addOptimizationListener(Consumer<OptimizationRecommendation> listener) {
        optimizationListeners.add(listener);
    }
    
    public void addReportListener(Consumer<AnalyticsReport> listener) {
        reportListeners.add(listener);
    }
    
    private void notifyOptimization(OptimizationRecommendation recommendation) {
        optimizationListeners.forEach(listener -> listener.accept(recommendation));
    }
    
    private void notifyReport(AnalyticsReport report) {
        reportListeners.forEach(listener -> listener.accept(report));
    }
    
    public void shutdown() {
        analyzer.shutdown();
    }
} 