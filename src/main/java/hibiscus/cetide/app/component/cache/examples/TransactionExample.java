package hibiscus.cetide.app.component.cache.examples;

import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.config.CacheConfig;
import hibiscus.cetide.app.component.cache.transaction.TransactionManager;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

public class TransactionExample {
    public static void main(String[] args) {
        ScheduledExecutorService cleanupService = Executors.newScheduledThreadPool(1);
        
        HibiscusCache<String, String> cache = new HibiscusCache<String, String>(
            cleanupService,
            new CacheConfig.Builder<String, String>().build()
        );
        
        TransactionManager<String, String> txManager = new TransactionManager<>(
            cache,
            3,      // 最大重试次数
            5000    // 超时时间(毫秒)
        );

        try {
            // 开始事务
            long txId = txManager.begin();

            try {
                // 事务操作
                txManager.put(txId, "key1", "value1");
                String value = txManager.get(txId, "key1");
                System.out.println("Value in transaction: " + value);

                // 提交事务
                boolean success = txManager.commit(txId);
                System.out.println("Transaction " + (success ? "committed" : "failed"));
            } catch (Exception e) {
                // 回滚事务
                txManager.rollback(txId);
                e.printStackTrace();
            }
        } finally {
            cleanupService.shutdown();
            cache.shutdown();
        }
    }
} 