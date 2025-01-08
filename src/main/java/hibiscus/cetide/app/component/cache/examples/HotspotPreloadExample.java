package hibiscus.cetide.app.component.cache.examples;

import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.config.CacheConfig;
import hibiscus.cetide.app.component.cache.hotspot.HotspotPreloader;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

public class HotspotPreloadExample {
    public static void main(String[] args) {
        ScheduledExecutorService cleanupService = Executors.newScheduledThreadPool(1);
        
        HibiscusCache<String, String> cache = new HibiscusCache<String, String>(
            cleanupService,
            new CacheConfig.Builder<String, String>().build()
        );
        
        // 创建热点数据预加载器
        HotspotPreloader<String, String> preloader = new HotspotPreloader<>(
            cache,
            key -> "value_" + key,  // 数据加载器
            4,                      // 预加载线程数
            1000                    // 热点阈值
        );

        try {
            // 记录访问
            for (int i = 0; i < 1000; i++) {
                preloader.recordAccess("key" + (i % 10), 1);  // 模拟热点访问
            }

            // 等待一段时间，让预加载发生
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanupService.shutdown();
            preloader.shutdown();
            cache.shutdown();
        }
    }
} 