package hibiscus.cetide.app.component.cache.hotspot;


import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.exception.CacheException;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.Set;
import java.util.function.Function;

public class HotspotPreloader<K, V> {
    private final ConcurrentSkipListMap<Double, K> hotspotMap;
    private final ExecutorService preloadExecutor;
    private final int preloadThreshold;
    private final Function<K, V> loader;
    private final HibiscusCache<K, V> cache;
    
    public HotspotPreloader(HibiscusCache<K, V> cache, Function<K, V> loader, int threads, int threshold) {
        this.hotspotMap = new ConcurrentSkipListMap<>();
        this.preloadExecutor = Executors.newFixedThreadPool(threads);
        this.preloadThreshold = threshold;
        this.loader = loader;
        this.cache = cache;
        
        startPreloadWorker();
    }
    
    public void recordAccess(K key, int accessCount) {
        double score = calculateHotspotScore(accessCount);
        hotspotMap.put(score, key);
        
        // 保持热点数据大小在阈值以内
        while (hotspotMap.size() > preloadThreshold) {
            hotspotMap.pollFirstEntry();
        }
    }
    
    private double calculateHotspotScore(int accessCount) {
        long now = System.nanoTime();
        // 使用访问次数和时间戳计算热度分数
        return accessCount * Math.exp(-0.000000001 * (now - System.nanoTime()));
    }
    
    private void startPreloadWorker() {
        preloadExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 获取热点数据并预加载
                    Set<K> hotKeys = new ConcurrentSkipListSet<>(hotspotMap.values());
                    for (K key : hotKeys) {
                        if (!cache.containsKey(key)) {
                            CompletableFuture.supplyAsync(() -> loader.apply(key))
                                .thenAccept(value -> {
                                    cache.put(key, value);
                                });
                        }
                    }
                    Thread.sleep(1000); // 控制预加载频率
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    public void shutdown() {
        preloadExecutor.shutdown();
    }
} 