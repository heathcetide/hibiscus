package hibiscus.cetide.app.component.cache.analytics;


import hibiscus.cetide.app.component.cache.config.CacheConfig;

public class AnalyticsConfig {
    private final int currentCacheSize;
    private final double hotKeyThreshold;
    private final long analyzeInterval;
    
    public AnalyticsConfig(CacheConfig config) {
        this.currentCacheSize = config.getMaxSize();
        this.hotKeyThreshold = 0.8;
        this.analyzeInterval = 60000; // 1分钟
    }
    
    public int getCurrentCacheSize() {
        return currentCacheSize;
    }
    
    public double getHotKeyThreshold() {
        return hotKeyThreshold;
    }
    
    public long getAnalyzeInterval() {
        return analyzeInterval;
    }
} 