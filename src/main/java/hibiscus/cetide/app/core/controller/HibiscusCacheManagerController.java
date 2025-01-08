package hibiscus.cetide.app.core.controller;

import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.config.CacheConfig;
import hibiscus.cetide.app.core.service.HibiscusCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存管理器使用示例
 */
@RestController
@RequestMapping("/api/hibiscus/cache")
public class HibiscusCacheManagerController {
    
    @Autowired
    private HibiscusCacheManager cacheManager;
    
    /**
     * 使用默认缓存
     */
    @PostMapping("/default/put")
    public void putToDefault(@RequestParam String key, @RequestParam String value) {
        HibiscusCache<Object, Object> defaultCache = cacheManager.getCache(HibiscusCacheManager.DEFAULT_CACHE_NAME);
        defaultCache.put(key, value);
    }
    
    @GetMapping("/default/get")
    public Object getFromDefault(@RequestParam String key) {
        HibiscusCache<Object, Object> defaultCache = cacheManager.getCache(HibiscusCacheManager.DEFAULT_CACHE_NAME);
        return defaultCache.get(key);
    }
    
    /**
     * 创建新的缓存实例
     */
    @PostMapping("/instances")
    public String createCache(@RequestParam String name, 
                            @RequestParam(defaultValue = "1000") int maxSize,
                            @RequestParam(defaultValue = "3600") int ttlSeconds) {
        try {
            CacheConfig<Object, Object> config = new CacheConfig.Builder<>()
                    .maxSize(maxSize)
                    .enableMetrics(true)
                    .compressionEnabled(true)
                    .defaultTTL(ttlSeconds, TimeUnit.SECONDS)
                    .evictionPolicy(CacheConfig.EvictionPolicy.LRU)
                    .performanceMonitorEnabled(true)
                    .build();
            
            cacheManager.createCache(name, config);
            return "Cache created: " + name;
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * 获取缓存实例的配置信息
     */
    @GetMapping("/instances/{name}/config")
    public Map<String, Object> getCacheConfig(@PathVariable String name) {
        HibiscusCache<Object, Object> cache = cacheManager.getCache(name);
        if (cache == null) {
            throw new IllegalArgumentException("Cache not found: " + name);
        }
        
        CacheConfig<Object, Object> config = cache.getConfig();
        Map<String, Object> configInfo = new LinkedHashMap<>();
        configInfo.put("maxSize", config.getMaxSize());
        configInfo.put("metricsEnabled", config.isMetricsEnabled());
        configInfo.put("compressionEnabled", config.isCompressionEnabled());
        configInfo.put("defaultTTL", config.getDefaultTTL());
        configInfo.put("evictionPolicy", config.getEvictionPolicy());
        configInfo.put("performanceMonitorEnabled", config.isPerformanceMonitorEnabled());
        
        return configInfo;
    }
    
    /**
     * 更新缓存实例的配置
     */
    @PutMapping("/instances/{name}/config")
    public String updateCacheConfig(@PathVariable String name,
                                  @RequestParam(required = false) Integer maxSize,
                                  @RequestParam(required = false) Integer ttlSeconds) {
        HibiscusCache<Object, Object> cache = cacheManager.getCache(name);
        if (cache == null) {
            return "Cache not found: " + name;
        }
        
        try {
            CacheConfig<Object, Object> oldConfig = cache.getConfig();
            CacheConfig<Object, Object> newConfig = new CacheConfig.Builder<>()
                    .maxSize(maxSize != null ? maxSize : oldConfig.getMaxSize())
                    .enableMetrics(oldConfig.isMetricsEnabled())
                    .compressionEnabled(oldConfig.isCompressionEnabled())
                    .defaultTTL(ttlSeconds != null ? ttlSeconds : oldConfig.getDefaultTTL(), TimeUnit.SECONDS)
                    .evictionPolicy(oldConfig.getEvictionPolicy())
                    .performanceMonitorEnabled(oldConfig.isPerformanceMonitorEnabled())
                    .build();
            
            cacheManager.updateCacheConfig(name, newConfig);
            return "Cache config updated: " + name;
        } catch (Exception e) {
            return "Error updating cache config: " + e.getMessage();
        }
    }
    
    /**
     * 获取缓存实例中的所有键
     */
    @GetMapping("/instances/{name}/keys")
    public Map<String, Object> getCacheKeys(@PathVariable String name) {
        HibiscusCache<Object, Object> cache = cacheManager.getCache(name);
        if (cache == null) {
            throw new IllegalArgumentException("Cache not found: " + name);
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("size", cache.size());
        
        // 获取所有键值对
        Map<Object, Object> entries = cache.getAll();
        Map<String, Object> keyMap = new LinkedHashMap<>();
        entries.forEach((key, value) -> keyMap.put(key.toString(), value));
        result.put("entries", keyMap);
        return result;
    }
    
    /**
     * 清空缓存实例中的所有数据
     */
    @DeleteMapping("/instances/{name}/clear")
    public String clearCache(@PathVariable String name) {
        HibiscusCache<Object, Object> cache = cacheManager.getCache(name);
        if (cache == null) {
            return "Cache not found: " + name;
        }
        
        cache.clear();
        return "Cache cleared: " + name;
    }
    
    /**
     * 批量操作缓存数据
     */
    @PostMapping("/instances/{name}/batch")
    public String batchOperation(@PathVariable String name,
                               @RequestBody Map<String, Object> entries) {
        HibiscusCache<Object, Object> cache = cacheManager.getCache(name);
        if (cache == null) {
            return "Cache not found: " + name;
        }
        
        try {
            // 转换Map类型
            Map<Object, Object> convertedMap = new HashMap<>();
            entries.forEach((key, value) -> convertedMap.put(key, value));
            cache.putAll(convertedMap);
            return "Batch operation completed: " + entries.size() + " entries processed";
        } catch (Exception e) {
            return "Error in batch operation: " + e.getMessage();
        }
    }
    
    /**
     * 使用指定的缓存实例
     */
    @PostMapping("/instances/{name}/put")
    public String putToCache(@PathVariable String name, 
                           @RequestParam String key, 
                           @RequestParam String value) {
        HibiscusCache<Object, Object> cache = cacheManager.getCache(name);
        if (cache == null) {
            return "Cache not found: " + name;
        }
        
        cache.put(key, value);
        return "Value stored in cache: " + name;
    }
    
    @GetMapping("/instances/{name}/get")
    public Object getFromCache(@PathVariable String name, 
                             @RequestParam String key) {
        HibiscusCache<Object, Object> cache = cacheManager.getCache(name);
        if (cache == null) {
            return "Cache not found: " + name;
        }
        
        return cache.get(key);
    }
    
    /**
     * 列出所有缓存实例
     */
    @GetMapping("/instances")
    public Map<String, Object> listCaches() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, HibiscusCache<Object, Object>> caches = cacheManager.getCaches();
        AtomicLong totalMemoryUsage = new AtomicLong();
        Map<String, Object> instancesInfo = new LinkedHashMap<>();
        caches.forEach((name, cache) -> {
            Map<String, Object> cacheInfo = new LinkedHashMap<>();
            long memoryUsage = cache.getMemoryUsage();

            cacheInfo.put("size", cache.size());
            cacheInfo.put("config", getCacheConfig(name));
            cacheInfo.put("memoryUsage", memoryUsage); // 添加内存占用信息

            instancesInfo.put(name, cacheInfo);
            totalMemoryUsage.addAndGet(memoryUsage);
        });
        result.put("totalInstances", caches.size());
        result.put("totalMemoryUsage", totalMemoryUsage); // 添加总内存占用
        result.put("instances", instancesInfo);
        return result;
    }
    
    /**
     * 删除缓存实例
     */
    @DeleteMapping("/instances/{name}")
    public String removeCache(@PathVariable String name) {
        try {
            cacheManager.removeCache(name);
            return "Cache removed: " + name;
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 搜索缓存实例
     * @param keyword 搜索关键词
     * @return 匹配的缓存实例信息
     */
    @GetMapping("/instances/search")
    public Map<String, Object> searchCaches(@RequestParam(required = false) String keyword) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, HibiscusCache<Object, Object>> allCaches = cacheManager.getCaches();
        Map<String, Object> instancesInfo = new LinkedHashMap<>();
        AtomicLong totalMemoryUsage = new AtomicLong();

        // 如果关键词为空，返回所有缓存实例
        if (keyword == null || keyword.trim().isEmpty()) {
            return listCaches();
        }

        // 过滤匹配的缓存实例
        allCaches.forEach((name, cache) -> {
            // 检查缓存名称是否包含关键词（不区分大小写）
            if (name.toLowerCase().contains(keyword.toLowerCase())) {
                Map<String, Object> cacheInfo = new LinkedHashMap<>();
                long memoryUsage = cache.getMemoryUsage();

                cacheInfo.put("size", cache.size());
                cacheInfo.put("config", getCacheConfig(name));
                cacheInfo.put("memoryUsage", memoryUsage);

                instancesInfo.put(name, cacheInfo);
                totalMemoryUsage.addAndGet(memoryUsage);
            }
        });

        result.put("totalInstances", instancesInfo.size());
        result.put("totalMemoryUsage", totalMemoryUsage);
        result.put("instances", instancesInfo);
        return result;
    }

    /**
     * 删除缓存实例中的特定条目
     */
    @DeleteMapping("/instances/{name}/entries/{key}")
    public Map<String, Object> deleteCacheEntry(@PathVariable String name, @PathVariable String key) {
        HibiscusCache<Object, Object> cache = cacheManager.getCache(name);
        if (cache == null) {
            throw new IllegalArgumentException("Cache not found: " + name);
        }
        cache.remove(key);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("success", true);
        return result;
    }

} 