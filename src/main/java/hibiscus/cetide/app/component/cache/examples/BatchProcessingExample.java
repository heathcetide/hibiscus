package hibiscus.cetide.app.component.cache.examples;

import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.batch.BatchProcessor;
import hibiscus.cetide.app.component.cache.config.CacheConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

public class BatchProcessingExample {
    public static void main(String[] args) {
        ScheduledExecutorService cleanupService = Executors.newScheduledThreadPool(1);
        
        HibiscusCache<String, String> cache = new HibiscusCache<>(
                cleanupService,
                new CacheConfig.Builder<String, String>().build()
        );
        
        // 创建批处理器
        BatchProcessor<String, String> batchProcessor = new BatchProcessor<>(
            cache,
            100,    // 批次大小
            1000,   // 队列容量
            4       // 处理线程数
        );

        // 准备批量数据
        Map<String, String> batch = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            batch.put("key" + i, "value" + i);
        }

        try {
            // 提交批量操作
            CompletableFuture<Map<String, String>> future = 
                batchProcessor.submitBatch(batch, BatchProcessor.OperationType.PUT);

            // 等待完成
            future.thenAccept(result -> 
                System.out.println("Batch operation completed: " + result.size() + " entries processed")
            );
        } finally {
            cleanupService.shutdown();
            cache.shutdown();
        }
    }
} 