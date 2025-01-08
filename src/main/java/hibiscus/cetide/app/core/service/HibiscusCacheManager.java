package hibiscus.cetide.app.core.service;

import hibiscus.cetide.app.config.BusinessCache;
import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.config.CacheConfig;
import hibiscus.cetide.app.component.cache.monitor.CacheMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;
import java.util.Collections;
import java.util.HashMap;

/**
 * 缓存管理器服务
 * 负责创建、管理和监控缓存实例
 */
@Service
public class HibiscusCacheManager {
    private static final Logger logger = LoggerFactory.getLogger(HibiscusCacheManager.class);
    /**
     * 默认缓存实例的名称
     */
    public static final String DEFAULT_CACHE_NAME = "default";

    private final ConcurrentMap<String, HibiscusCache<Object, Object>> caches = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CacheMonitorService<Object, Object>> monitors = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupService;

    public HibiscusCacheManager() {
        this.cleanupService = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "cache-cleanup-thread");
                t.setDaemon(true);
                return t;
            }
        );
    }
    
    @PostConstruct
    public void init() {
        // 创建默认缓存实例
        createDefaultCache();
        logger.info("HibiscusCacheManager initialized with default cache instance");
    }
    
    private void createDefaultCache() {
        CacheConfig<Object, Object> config = new CacheConfig.Builder<>()
                .maxSize(10000)
                .enableMetrics(true)
                .compressionEnabled(true)
                .asyncWrite(true)
                .defaultTTL(1, TimeUnit.HOURS)
                .evictionPolicy(CacheConfig.EvictionPolicy.LRU)
                .performanceMonitorEnabled(true)
                .build();
        createCache(DEFAULT_CACHE_NAME, config);
    }
    
    /**
     * 创建新的缓存实例
     * @param name 缓存实例名称
     * @param config 缓存配置
     * @return 创建的缓存实例
     * @throws IllegalArgumentException 如果缓存名称已存在
     */
    public HibiscusCache<Object, Object> createCache(String name, CacheConfig<Object, Object> config) {
        if (caches.containsKey(name)) {
            throw new IllegalArgumentException("Cache with name '" + name + "' already exists");
        }
        
        // 创建缓存实例
        HibiscusCache<Object, Object> cache = new HibiscusCache<>(cleanupService, config);
        caches.put(name, cache);
        
        // 创建并启动监控服务
        CacheMonitorService<Object, Object> monitor = 
            new CacheMonitorService<>(cache, 5, TimeUnit.SECONDS);
        monitor.start();
        monitors.put(name, monitor);
        
        logger.info("Created new cache instance: {}", name);
        return cache;
    }
    
    /**
     * 获取缓存实例
     * @param name 缓存实例名称
     * @return 缓存实例，如果不存在返回null
     */
    public HibiscusCache<Object, Object> getCache(String name) {
        return caches.get(name);
    }
    
    /**
     * 获取所有缓存实例
     */
    public Map<String, HibiscusCache<Object, Object>> getCaches() {
        return Collections.unmodifiableMap(new HashMap<>(caches));
    }
    
    /**
     * 获取所有监控服务
     */
    public Map<String, CacheMonitorService<Object, Object>> getMonitors() {
        return Collections.unmodifiableMap(new HashMap<>(monitors));
    }
    
    /**
     * 移除缓存实例
     * @param name 缓存实例名称
     * @throws IllegalArgumentException 如果尝试移除默认缓存
     */
    public void removeCache(String name) {
        if (DEFAULT_CACHE_NAME.equals(name)) {
            throw new IllegalArgumentException("Cannot remove default cache");
        }
        
        HibiscusCache<Object, Object> cache = caches.remove(name);
        if (cache != null) {
            cache.shutdown();
        }
        
        CacheMonitorService<Object, Object> monitor = monitors.remove(name);
        if (monitor != null) {
            monitor.shutdown();
        }
        
        logger.info("Removed cache instance: {}", name);
    }
    
    @PreDestroy
    public void shutdown() {
        // 关闭所有监控服务
        monitors.values().forEach(CacheMonitorService::shutdown);
        monitors.clear();
        
        // 关闭所有缓存实例
        caches.values().forEach(HibiscusCache::shutdown);
        caches.clear();
        
        // 关闭清理服务
        cleanupService.shutdown();
        try {
            if (!cleanupService.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupService.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("HibiscusCacheManager shut down");
    }
    
    /**
     * 更新缓存实例的配置
     * @param name 缓存实例名称
     * @param newConfig 新的配置
     * @throws IllegalArgumentException 如果缓存不存在
     */
    public void updateCacheConfig(String name, CacheConfig<Object, Object> newConfig) {
        HibiscusCache<Object, Object> cache = getCache(name);
        if (cache == null) {
            throw new IllegalArgumentException("Cache not found: " + name);
        }
        
        // 创建新的缓存实例
        HibiscusCache<Object, Object> newCache = new HibiscusCache<>(cleanupService, newConfig);
        
        // 复制原缓存的数据
        cache.getKeys().forEach(key -> {
            Object value = cache.get(key);
            if (value != null) {
                newCache.put(key, value);
            }
        });
        
        // 关闭旧的监控服务
        CacheMonitorService<Object, Object> oldMonitor = monitors.remove(name);
        if (oldMonitor != null) {
            oldMonitor.shutdown();
        }
        
        // 关闭旧的缓存实例
        cache.shutdown();
        
        // 替换为新的缓存实例
        caches.put(name, newCache);
        
        // 创建并启动新的监控服务
        CacheMonitorService<Object, Object> newMonitor = 
            new CacheMonitorService<>(newCache, 5, TimeUnit.SECONDS);
        newMonitor.start();
        monitors.put(name, newMonitor);
        
        logger.info("Updated cache config for instance: {}", name);
    }

    // 修改 getBusinessCache 方法，添加正确的泛型支持
    public <K, V> BusinessCache<K, V> getBusinessCache(String name) {
        HibiscusCache<Object, Object> cache = caches.get(name);
        if (cache == null) {
            return null;
        }
        return new BusinessCache<K, V>(cache);
    }
} 