package hibiscus.cetide.app.component.cache.multilevel;

import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MultiLevelCache<K, V> {
    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCache.class);
    private final List<CacheLevel<K, V>> levels;
    
    public static class CacheLevel<K, V> {
        final HibiscusCache<K, V> cache;
        final int priority;
        final long maxLatency;  // 最大访问延迟（纳秒）
        
        public CacheLevel(HibiscusCache<K, V> cache, int priority, long maxLatency) {
            this.cache = cache;
            this.priority = priority;
            this.maxLatency = maxLatency;
        }
    }
    
    public MultiLevelCache() {
        this.levels = new ArrayList<>();
    }
    
    public void addLevel(HibiscusCache<K, V> cache, int priority, long maxLatency) {
        levels.add(new CacheLevel<>(cache, priority, maxLatency));
        levels.sort((a, b) -> Integer.compare(b.priority, a.priority));
    }
    
    public V get(K key) throws IOException, ClassNotFoundException, CacheException {
        long startTime = System.nanoTime();
        
        for (CacheLevel<K, V> level : levels) {
            V value = level.cache.get(key);
            if (value != null) {
                // 异步更新高优先级缓存
                updateHigherLevels(key, value, level.priority);
                return value;
            }
            
            long currentLatency = System.nanoTime() - startTime;
            if (currentLatency > level.maxLatency) {
                break;  // 超时，停止查找低优先级缓存
            }
        }
        
        return null;
    }
    
    private void updateHigherLevels(K key, V value, int currentPriority) {
        CompletableFuture.runAsync(() -> {
            for (CacheLevel<K, V> level : levels) {
                if (level.priority > currentPriority) {
                    level.cache.put(key, value);
                }
            }
        });
    }
    
    /**
     * 向多级缓存中写入数据
     * 数据会被写入所有层级的缓存中
     */
    public void put(K key, V value) throws IOException {
        if (key == null || value == null) {
            return;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // 并行写入所有缓存层级
        for (CacheLevel<K, V> level : levels) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                level.cache.put(key, value);
            });
            futures.add(future);
        }

        // 等待所有写入操作完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            throw new IOException("Failed to write to some cache levels", e);
        }
    }

    /**
     * 从多级缓存中删除数据
     */
    public void remove(K key) {
        if (key == null) {
            return;
        }

        // 并行从所有缓存层级中删除
        CompletableFuture.allOf(
            levels.stream()
                .map(level -> CompletableFuture.runAsync(() -> {
                    try {
                        level.cache.remove(key);
                    } catch (Exception e) {
                        logger.error("Failed to remove from cache level with priority {}", level.priority, e);
                    }
                }))
                .toArray(CompletableFuture[]::new)
        ).join();
    }

    /**
     * 清空所有缓存层级
     */
    public void clear() {
        CompletableFuture.allOf(
            levels.stream()
                .map(level -> CompletableFuture.runAsync(() -> {
                    try {
                        level.cache.clear();
                    } catch (Exception e) {
                        logger.error("Failed to clear cache level with priority {}", level.priority, e);
                    }
                }))
                .toArray(CompletableFuture[]::new)
        ).join();
    }
} 