package hibiscus.cetide.app.component.cache.examples;

import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

public class BasicCacheExample {
    private static Logger logger = LoggerFactory.getLogger(BasicCacheExample.class);

//    private static Map<String, Object> hash = new ConcurrentHashMap<>();
//
//    public static void main(String[] args) {
//        long startTime = System.nanoTime();
//        hash.put("key1", "value1");
//        logger.info("存: {}",System.nanoTime()-startTime);
//        startTime = System.nanoTime();
//        Object o = hash.get("key1");
//        logger.info("取: {}",System.nanoTime()-startTime);
//    }
    public static void main(String[] args) throws IOException {
        // 创建清理服务
        ScheduledExecutorService cleanupService = Executors.newScheduledThreadPool(1);

        // 创建基本配置
        CacheConfig<String, String> config = new CacheConfig.Builder<String, String>()
            .maxSize(10000)
            .defaultTTL(30, TimeUnit.MINUTES)
            .enableMetrics(false)
            .asyncWrite(false)
            .build();

        // 创建缓存实例
        HibiscusCache<String, String> cache = new HibiscusCache<>(cleanupService, config);

        try {
            long startTime = System.nanoTime();
            cache.put("key1", "value1");
            logger.info("存: {}",System.nanoTime()-startTime);
            startTime = System.nanoTime();
            String value = cache.get("key1");
            logger.info("取: {}",System.nanoTime()-startTime);
            System.out.println("Retrieved value: " + value);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanupService.shutdown(); // 确保清理服务也被关闭
            cache.shutdown();
        }

        // 自动适应数据量的变化
        for (int i = 0; i < 1_000_000; i++) {
            cache.put("key" + i, "value" + i);
        }
    }
} 