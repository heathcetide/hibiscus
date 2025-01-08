package hibiscus.cetide.app.component.cache.examples;

import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.config.CacheConfig;
import hibiscus.cetide.app.component.cache.monitor.CacheMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CacheMonitoringExample {
    private static final Logger logger = LoggerFactory.getLogger(CacheMonitoringExample.class);

    public static void main(String[] args) {
        // 创建清理服务
        ScheduledExecutorService cleanupService = Executors.newScheduledThreadPool(1);

        try {
            // 创建缓存配置，启用监控功能
            CacheConfig<String, String> config = new CacheConfig.Builder<String, String>()
                .maxSize(1000)
                .enableMetrics(true)
                .enablePrediction(true)
                .performanceMonitorEnabled(true)
                .build();

            // 创建缓存实例
            HibiscusCache<String, String> cache = new HibiscusCache<>(cleanupService, config);

            // 模拟缓存操作
            simulateCacheOperations(cache);

            // 创建并启动监控服务
            CacheMonitorService<String, String> monitorService = 
                new CacheMonitorService<>(cache, 5, TimeUnit.SECONDS);
            monitorService.start();
            
//            // 创建监控端点
//            CacheMonitorEndpoint endpoint = new CacheMonitorEndpoint(monitorService);
//
////             定期获取缓存状态
//            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//            scheduler.scheduleAtFixedRate(() -> {
//                String status = endpoint.getHealthStatus();
//                System.out.println("Cache Status: " + status);
//            }, 0, 10, TimeUnit.SECONDS);

            // 运行一段时间后关闭
            Thread.sleep(30000); // 运行30秒
            monitorService.shutdown();
//            scheduler.shutdown();

        } catch (Exception e) {
            logger.error("示例运行时发生错误", e);
        } finally {
            cleanupService.shutdown();
        }
    }

    private static void simulateCacheOperations(HibiscusCache<String, String> cache) {
        // 模拟写入操作
        for (int i = 0; i < 1000; i++) {
            try {
                cache.put("key" + i, "value" + i);
                
                // 模拟一些延迟
                if (i % 100 == 0) {
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                logger.error("写入操作失败", e);
            }
        }

        // 模拟读取操作，包括命中和未命中的情况
        for (int i = 0; i < 2000; i++) {
            try {
                // 80%的概率访问已存在的键
                int key = (int) (Math.random() * (Math.random() < 0.8 ? 1000 : 2000));
                String value = cache.get("key" + key);
                
                // 模拟一些延迟
                if (i % 200 == 0) {
                    Thread.sleep(50);
                }
                
                if (value != null) {
                    logger.debug("命中: key{} = {}", key, value);
                } else {
                    logger.debug("未命中: key{}", key);
                }
            } catch (Exception e) {
                logger.error("读取操作失败", e);
            }
        }
    }

    private static void printCacheStats(HibiscusCache<String, String> cache) {
        logger.info("=== 缓存统计信息 ===");
        logger.info("缓存大小: {}", cache.size());
        
        // 获取命中率统计
        logger.info("命中次数: {}", cache.getStats().getHitCount());
        logger.info("未命中次数: {}", cache.getStats().getMissCount());
        logger.info("命中率: {}%", String.format("%.2f", cache.getStats().getHitRate() * 100));
        
        // 获取驱逐统计
        logger.info("驱逐次数: {}", cache.getStats().getEvictionCount());
        logger.info("过期次数: {}", cache.getStats().getExpirationCount());

        // 内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        logger.info("内存使用: {}MB", usedMemory);
        
        // 打印分隔线
        logger.info("========================");
    }
} 