package hibiscus.cetide.app.component.cache.examples;

import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.config.CacheConfig;
import hibiscus.cetide.app.component.cache.exception.CacheException;
import hibiscus.cetide.app.component.cache.monitor.CacheMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 创建缓存实例的示例
 * 
 * 方式1：通过配置文件（推荐）
 * 在 application.yml 中配置：
 * 
 * hibiscus:
 *   cache:
 *     enabled: true
 *     monitor:
 *       enabled: true
 *       interval: 5
 *       time-unit: SECONDS
 *     instances:
 *       # 用户缓存配置
 *       userCache:
 *         max-size: 1000
 *         metrics-enabled: true
 *         compression-enabled: false
 *         time-to-live: 3600
 *         time-unit: SECONDS
 *         eviction-policy: LRU
 *       # 产品缓存配置
 *       productCache:
 *         max-size: 2000
 *         compression-enabled: true
 *         time-to-live: 1800
 *         time-unit: SECONDS
 *         eviction-policy: LFU
 *       # 订单缓存配置
 *       orderCache:
 *         max-size: 5000
 *         metrics-enabled: true
 *         compression-enabled: true
 *         time-to-live: 7200
 *         time-unit: SECONDS
 *         eviction-policy: W_TINYLFU
 */
//@Service
public class CreateCacheExample {
    
//    // 方式1：通过自动配置注入缓存实例
////    @Autowired
//    private Map<String, HibiscusCache<Object, Object>> caches;
//
//    /**
//     * 方式2：编程方式创建缓存实例
//     */
//    public HibiscusCache<String, Object> createProgrammatically() {
//        // 1. 创建清理服务
//        ScheduledExecutorService cleanupService = Executors.newScheduledThreadPool(1);
//
//        // 2. 创建缓存配置
//        CacheConfig config = new CacheConfig.Builder<String, Object>()
//                .maxSize(1000)                          // 最大容量
//                .enableMetrics(true)                    // 启用指标收集
//                .compressionEnabled(true)               // 启用压缩
//                .asyncWrite(true)                       // 启用异步写入
//                .defaultTTL(1, TimeUnit.HOURS)          // 设置TTL为1小时
//                .evictionPolicy(CacheConfig.EvictionPolicy.LRU)  // 使用LRU淘汰策略
//                .performanceMonitorEnabled(true)        // 启用性能监控
//                .monitoringWindow(5, TimeUnit.MINUTES)  // 监控窗口为5分钟
//                .build();
//
//        // 3. 创建缓存实例
//        HibiscusCache<String, Object> cache = new HibiscusCache<>(cleanupService, config);
//
//        // 4. 创建并启动监控服务（可选）
//        CacheMonitorService<String, Object> monitor =
//            new CacheMonitorService<>(cache, 5, TimeUnit.SECONDS);
//        monitor.start();
//
//        return cache;
//    }
    
    /**
     * 使用示例
     */
//    public void usageExample() throws CacheException, IOException {
//        // 方式1：使用配置文件创建的缓存
//        HibiscusCache<Object, Object> userCache = caches.get("userCache");
//        if (userCache != null) {
//            userCache.put("user:1", new User("1", "张三"));
//            User user = (User) userCache.get("user:1");
//        }
//
//        // 方式2：使用编程方式创建的缓存
//        HibiscusCache<String, Object> customCache = createProgrammatically();
//        customCache.put("key1", "value1");
//        String value = (String) customCache.get("key1");
//
//        // 注意：编程方式创建的缓存需要手动管理生命周期
//        // 在应用关闭时需要调用 shutdown
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            customCache.shutdown();
//        }));
//    }
    
//    // 示例实体类
//    public static class User {
//        private String id;
//        private String name;
//
//        public User(String id, String name) {
//            this.id = id;
//            this.name = name;
//        }
//
//        // getters and setters
//    }
} 