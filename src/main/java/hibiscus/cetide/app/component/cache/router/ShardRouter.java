package hibiscus.cetide.app.component.cache.router;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class ShardRouter<K> {
    private final int shardCount;
    private final ConcurrentHashMap<Integer, AtomicInteger> shardLoads;
    private final Function<K, Integer> hashFunction;
    private final double loadBalanceThreshold;
    
    public ShardRouter(int shardCount, Function<K, Integer> hashFunction) {
        this.shardCount = shardCount;
        this.shardLoads = new ConcurrentHashMap<>();
        this.hashFunction = hashFunction;
        this.loadBalanceThreshold = 1.2; // 负载不平衡阈值
        
        for (int i = 0; i < shardCount; i++) {
            shardLoads.put(i, new AtomicInteger(0));
        }
    }
    
    public int getShardId(K key) {
        int baseShardId = Math.abs(hashFunction.apply(key) % shardCount);
        
        // 检查负载是否均衡
        if (needsRebalancing()) {
            return findLeastLoadedShard();
        }
        
        return baseShardId;
    }
    
    public void recordAccess(int shardId) {
        shardLoads.get(shardId).incrementAndGet();
    }
    
    private boolean needsRebalancing() {
        int minLoad = Integer.MAX_VALUE;
        int maxLoad = 0;
        
        for (AtomicInteger load : shardLoads.values()) {
            int currentLoad = load.get();
            minLoad = Math.min(minLoad, currentLoad);
            maxLoad = Math.max(maxLoad, currentLoad);
        }
        
        return maxLoad > minLoad * loadBalanceThreshold;
    }
    
    private int findLeastLoadedShard() {
        int minLoad = Integer.MAX_VALUE;
        int selectedShard = 0;
        for (Map.Entry<Integer, AtomicInteger> entry : shardLoads.entrySet()) {
            int currentLoad = entry.getValue().get();
            if (currentLoad < minLoad) {
                minLoad = currentLoad;
                selectedShard = entry.getKey();
            }
        }

        return selectedShard;
    }
    
    public void resetLoads() {
        shardLoads.values().forEach(counter -> counter.set(0));
    }
} 