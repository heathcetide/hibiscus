package hibiscus.cetide.app.component.cache.analytics;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

public class CacheOptimizer<K> {
    private final AnalyticsConfig config;
    
    public CacheOptimizer(AnalyticsConfig config) {
        this.config = config;
    }
    
    public OptimizationResult optimize(
        ConcurrentSkipListMap<Double, Set<K>> hotKeys,
        Map<K, CacheAnalyzer.AccessPattern> accessPatterns,
        AnalyticsConfig config) {
        
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        
        // 分析缓存大小
        analyzeCacheSize(hotKeys, recommendations);
        
        // 分析TTL设置
        analyzeTTL(accessPatterns, recommendations);
        
        // 分析预加载策略
        analyzePreloading(hotKeys, recommendations);
        
        return new OptimizationResult(recommendations);
    }
    
    private Map<String, Object> createMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
    
    private void analyzeCacheSize(
        ConcurrentSkipListMap<Double, Set<K>> hotKeys,
        List<OptimizationRecommendation> recommendations) {
        
        int totalHotKeys = hotKeys.values().stream()
                                 .mapToInt(Set::size)
                                 .sum();
        
        if (totalHotKeys > config.getCurrentCacheSize() * 0.8) {
            recommendations.add(new OptimizationRecommendation(
                OptimizationType.INCREASE_CACHE_SIZE,
                "Increase cache size by 50% to accommodate hot keys",
                createMap("increase_factor", 1.5)
            ));
        }
    }
    
    private void analyzeTTL(
        Map<K, CacheAnalyzer.AccessPattern> accessPatterns,
        List<OptimizationRecommendation> recommendations) {
        
        double avgHitRate = accessPatterns.values().stream()
            .mapToDouble(pattern -> {
                long total = pattern.accessCount.get();
                long misses = pattern.missCount.get();
                return total == 0 ? 0.0 : (total - misses) / (double) total;
            })
            .average()
            .orElse(0.0);
        
        if (avgHitRate < 0.8) {
            recommendations.add(new OptimizationRecommendation(
                OptimizationType.ADJUST_TTL,
                "Increase TTL to improve hit rate",
                createMap("ttl_factor", 2.0)
            ));
        }
    }
    
    private void analyzePreloading(
        ConcurrentSkipListMap<Double, Set<K>> hotKeys,
        List<OptimizationRecommendation> recommendations) {
        
        Set<K> topHotKeys = hotKeys.descendingMap()
                                  .values()
                                  .stream()
                                  .limit(10)
                                  .flatMap(Set::stream)
                                  .collect(Collectors.toSet());
        
        if (!topHotKeys.isEmpty()) {
            recommendations.add(new OptimizationRecommendation(
                OptimizationType.PRELOAD_KEYS,
                "Preload top hot keys",
                createMap("keys", topHotKeys)
            ));
        }
    }
} 