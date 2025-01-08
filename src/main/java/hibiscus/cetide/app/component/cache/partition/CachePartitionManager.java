package hibiscus.cetide.app.component.cache.partition;

import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.config.CacheConfig;
import hibiscus.cetide.app.component.cache.exception.CacheException;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

public class CachePartitionManager<K, V> {
    private final int partitionCount;
    private final ConcurrentHashMap<Integer, HibiscusCache<K, V>> partitions;
    private final Function<K, Integer> partitioner;
    private final ScheduledExecutorService cleanupService;
    
    public CachePartitionManager(int partitionCount, Function<K, Integer> partitioner) {
        this.partitionCount = partitionCount;
        this.partitioner = partitioner;
        this.partitions = new ConcurrentHashMap<>();
        this.cleanupService = Executors.newScheduledThreadPool(1);
        
        for (int i = 0; i < partitionCount; i++) {
            CacheConfig<K, V> config = new CacheConfig.Builder<K, V>()
                .maxSize(10000 / partitionCount)
                .build();
            partitions.put(i, new HibiscusCache<>(cleanupService, config));
        }
    }
    
    public V get(K key) throws CacheException {
        int partition = getPartition(key);
        return partitions.get(partition).get(key);
    }
    
    public void put(K key, V value) throws IOException {
        int partition = getPartition(key);
        partitions.get(partition).put(key, value);
    }
    
    private int getPartition(K key) {
        return Math.abs(partitioner.apply(key) % partitionCount);
    }
    
    public void rebalance() {
        // 实现分区重平衡逻辑
    }
    
    public void shutdown() {
        cleanupService.shutdown();
        partitions.values().forEach(server -> {
            // 添加关闭服务器的逻辑
        });
    }
} 