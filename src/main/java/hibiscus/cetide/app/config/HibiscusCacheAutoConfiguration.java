package hibiscus.cetide.app.config;

import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.config.CacheConfig;
import hibiscus.cetide.app.component.cache.monitor.CacheMonitorService;
import hibiscus.cetide.app.core.model.HibiscusCacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableConfigurationProperties(HibiscusCacheProperties.class)
@ConditionalOnProperty(prefix = "hibiscus.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HibiscusCacheAutoConfiguration {

    private final HibiscusCacheProperties properties;
    
    public HibiscusCacheAutoConfiguration(HibiscusCacheProperties properties) {
        this.properties = properties;
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService cacheCleanupService() {
        return Executors.newScheduledThreadPool(1);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public Map<String, HibiscusCache<Object, Object>> hibiscusCaches(ScheduledExecutorService cleanupService) {
        Map<String, HibiscusCache<Object, Object>> caches = new HashMap<>();
        
        // 如果没有配置实例，创建一个默认实例
        if (properties.getInstances().isEmpty()) {
            caches.put("default", createDefaultCache(cleanupService));
        } else {
            // 创建配置的所有缓存实例
            properties.getInstances().forEach((name, props) -> 
                caches.put(name, createCache(name, props, cleanupService)));
        }
        
        return caches;
    }
    
    @Bean
    @SuppressWarnings("unchecked")
    public Map<String, CacheMonitorService<Object, Object>> cacheMonitors(
            Map<String, HibiscusCache<Object, Object>> caches) {
        Map<String, CacheMonitorService<Object, Object>> monitors = new HashMap<>();
        
        if (properties.getMonitor().isEnabled()) {
            caches.forEach((name, cache) -> {
                CacheMonitorService<Object, Object> monitor = new CacheMonitorService<>(
                    cache,
                    properties.getMonitor().getInterval(),
                    properties.getMonitor().getTimeUnit()
                );
                monitor.start();
                monitors.put(name, monitor);
            });
        }
        
        return monitors;
    }
    
    private HibiscusCache<Object, Object> createDefaultCache(ScheduledExecutorService cleanupService) {
        CacheConfig<Object, Object> config = new CacheConfig.Builder<>()
                .maxSize(1000)
                .enableMetrics(true)
                .build();
        return new HibiscusCache<>(cleanupService, config);
    }
    
    private HibiscusCache<Object, Object> createCache(
            String name,
            HibiscusCacheProperties.CacheInstanceProperties props,
            ScheduledExecutorService cleanupService) {
        CacheConfig<Object, Object> config = new CacheConfig.Builder<>()
                .maxSize(props.getMaxSize())
                .enableMetrics(props.isMetricsEnabled())
                .compressionEnabled(props.isCompressionEnabled())
                .asyncWrite(props.isAsyncWriteEnabled())
                .defaultTTL(props.getTtl(), props.getTimeUnit())
                .evictionPolicy(props.getEvictionPolicy())
                .build();
        return new HibiscusCache<>(cleanupService, config);
    }
} 