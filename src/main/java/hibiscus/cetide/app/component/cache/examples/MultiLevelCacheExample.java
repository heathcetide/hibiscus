package hibiscus.cetide.app.component.cache.examples;

import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.config.CacheConfig;
import hibiscus.cetide.app.component.cache.multilevel.MultiLevelCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

public class MultiLevelCacheExample {
    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCacheExample.class);
    
    public static void main(String[] args) {
        ScheduledExecutorService cleanupService = Executors.newScheduledThreadPool(1);
        
        MultiLevelCache<String, String> multiLevelCache = new MultiLevelCache<>();

        // L1缓存：容量小，速度快
        HibiscusCache<String, String> l1Cache = new HibiscusCache<String, String>(
            cleanupService,
            new CacheConfig.Builder<String, String>()
                .maxSize(1000)
                .defaultTTL(1, TimeUnit.MINUTES)  // 较短的TTL
                .build()
        );

        // L2缓存：容量大，速度相对慢
        HibiscusCache<String, String> l2Cache = new HibiscusCache<String, String>(
            cleanupService,
            new CacheConfig.Builder<String, String>()
                .maxSize(10000)
                .defaultTTL(10, TimeUnit.MINUTES)  // 较长的TTL
                .build()
        );

        // 添加缓存层级
        multiLevelCache.addLevel(l1Cache, 2, TimeUnit.MICROSECONDS.toNanos(100));  // L1优先级高
        multiLevelCache.addLevel(l2Cache, 1, TimeUnit.MILLISECONDS.toNanos(1));    // L2优先级低

        try {
            // 写入数据
            logger.info("Writing data to cache...");
            multiLevelCache.put("key1", "value1");
            multiLevelCache.put("key2", "value2");
            multiLevelCache.put("key3", "value3");

            // 从缓存读取数据
            logger.info("Reading data from cache...");
            String value1 = multiLevelCache.get("key1");
            logger.info("Retrieved value1: {}", value1);  // 从L1缓存获取
            
            // 模拟一些延迟，让key1在L1中过期
            Thread.sleep(10 * 1000);  // 等待70秒
            
            String value1Again = multiLevelCache.get("key1");
            logger.info("Retrieved value1 again: {}", value1Again);  // 从L2缓存获取
            
            // 模拟缓存未命中的情况
            String nonExistentValue = multiLevelCache.get("nonexistent");
            logger.info("Retrieved non-existent value: {}", nonExistentValue);  // 返回null
            
        } catch (Exception e) {
            logger.error("Error occurred", e);
        } finally {
            cleanupService.shutdown();
        }
    }
} 