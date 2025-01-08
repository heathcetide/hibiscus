package hibiscus.cetide.app.core.model;

import hibiscus.cetide.app.component.cache.config.CacheConfig;
import hibiscus.cetide.app.core.service.HibiscusCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ConfigurationProperties(prefix = "hibiscus.cache")
public class HibiscusCacheProperties {

    @Autowired
    private HibiscusCacheManager cacheManager;

    private boolean enabled = true;
    private Map<String, CacheInstanceProperties> instances = new HashMap<>();
    private MonitorProperties monitor = new MonitorProperties();

    @PostConstruct
    public void initializeCaches() {
        instances.forEach((name, props) -> {
            CacheConfig config = new CacheConfig.Builder<String, Object>()
                    .maxSize(props.getMaxSize())                          // 最大容量
                    .enableMetrics(props.metricsEnabled)                    // 启用指标收集
                    .compressionEnabled(props.compressionEnabled)               // 启用压缩
                    .asyncWrite(props.asyncWriteEnabled)                       // 启用异步写入
                    .defaultTTL(props.ttl, props.timeUnit)          // 设置TTL为1小时
                    .evictionPolicy(props.policy)  // 使用LRU淘汰策略
                    .performanceMonitorEnabled(true)        // 启用性能监控
                    .monitoringWindow(5, TimeUnit.MINUTES)  // 监控窗口为5分钟
                    .build();
            cacheManager.createCache(name, config);
        });
    }

    public static class CacheInstanceProperties {
        // 配置中的 max-size
        private int maxSize = 1000;

        // 配置中的 metrics-enabled
        private boolean metricsEnabled = true;

        // 配置中的 compression-enabled
        private boolean compressionEnabled = false;

        // 配置中的 async-write-enabled
        private boolean asyncWriteEnabled = false;

        // 配置中的 ttl (默认单位为毫秒，使用 timeToLive 和 timeUnit 组合)
        private long ttl = 0;

        // 配置中的 time-unit，默认值为 SECONDS
        private TimeUnit timeUnit = TimeUnit.SECONDS;

        // 配置中的 eviction-policy，默认为 LRU 策略
        private CacheConfig.EvictionPolicy policy = CacheConfig.EvictionPolicy.LRU;

        // getters and setters
        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public boolean isMetricsEnabled() {
            return metricsEnabled;
        }

        public void setMetricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
        }

        public boolean isCompressionEnabled() {
            return compressionEnabled;
        }

        public void setCompressionEnabled(boolean compressionEnabled) {
            this.compressionEnabled = compressionEnabled;
        }

        public boolean isAsyncWriteEnabled() {
            return asyncWriteEnabled;
        }

        public void setAsyncWriteEnabled(boolean asyncWriteEnabled) {
            this.asyncWriteEnabled = asyncWriteEnabled;
        }

        public long getTtl() {
            return ttl;
        }

        public void setTtl(long ttl) {
            this.ttl = ttl;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public void setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
        }

        public CacheConfig.EvictionPolicy getEvictionPolicy() {
            return policy;
        }

        public void setEvictionPolicy(CacheConfig.EvictionPolicy evictionPolicy) {
            this.policy = evictionPolicy;
        }


    }

    public static class MonitorProperties {
        private boolean enabled = true;
        private long interval = 5;
        private TimeUnit timeUnit = TimeUnit.SECONDS;

        // getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getInterval() {
            return interval;
        }

        public void setInterval(long interval) {
            this.interval = interval;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public void setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
        }
    }

    // getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, CacheInstanceProperties> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, CacheInstanceProperties> instances) {
        this.instances = instances;
    }

    public MonitorProperties getMonitor() {
        return monitor;
    }

    public void setMonitor(MonitorProperties monitor) {
        this.monitor = monitor;
    }
} 