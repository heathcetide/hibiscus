package hibiscus.cetide.app.component.cache.storage;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class ShardedStorage<K, V> {
    private final FastHashTable<K, V>[] shards;
    private final int mask;
    
    @SuppressWarnings("unchecked")
    public ShardedStorage(int shardCount) {
        // 确保分片数是2的幂
        int normalizedCount = Integer.highestOneBit(shardCount - 1) << 1;
        this.shards = new FastHashTable[normalizedCount];
        this.mask = normalizedCount - 1;
        
        for (int i = 0; i < normalizedCount; i++) {
            shards[i] = new FastHashTable<>(1024);
        }
    }
    
    public V put(K key, V value) {
        return getShard(key).put(key, value);
    }
    
    public V get(K key) {
        return getShard(key).get(key);
    }
    
    private FastHashTable<K, V> getShard(K key) {
        // 使用位运算快速定位分片
        return shards[key.hashCode() & mask];
    }

    public void remove(K key) {
        getShard(key).remove(key);
    }

    public void clear() {
        for (FastHashTable<K, V> shard : shards) {
            shard.clear();
        }
    }

    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entries = new HashSet<>();
        for (FastHashTable<K, V> shard : shards) {
            entries.addAll(shard.entrySet());
        }
        return entries;
    }

    public Set<K> keySet() {
        Set<K> keys = new HashSet<>();
        for (FastHashTable<K, V> shard : shards) {
            keys.addAll(shard.keySet());
        }
        return keys;
    }

    public void forEach(BiConsumer<K, V> action) {
        for (FastHashTable<K, V> shard : shards) {
            shard.forEach(action);
        }
    }
} 