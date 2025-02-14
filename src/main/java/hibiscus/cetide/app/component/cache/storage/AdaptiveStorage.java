package hibiscus.cetide.app.component.cache.storage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.Map;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.stream.Collectors;

public class AdaptiveStorage<K, V> {
    private static final int SMALL_THRESHOLD = 100_000;
    private static final int MEDIUM_THRESHOLD = 1_000_000;
    
    private volatile StorageType currentType;
    private final AtomicInteger size = new AtomicInteger(0);
    
    // 不同的存储实现
    private ConcurrentHashMap<K, V> smallStorage;
    private ShardedStorage<K, V> mediumStorage;
    private OffHeapStorage<K> offHeapStorage;
    private final Serializer<V> serializer;
    
    public enum StorageType {
        SMALL,   // ConcurrentHashMap
        MEDIUM,  // 分片存储
        LARGE    // 堆外内存
    }
    
    public AdaptiveStorage(Serializer<V> serializer) {
        this.serializer = serializer;
        this.currentType = StorageType.SMALL;
        this.smallStorage = new ConcurrentHashMap<>();
    }
    
    public void put(K key, V value) {
        checkAndUpgrade();
        
        switch (currentType) {
            case SMALL:
                smallStorage.put(key, value);
                break;
            case MEDIUM:
                mediumStorage.put(key, value);
                break;
            case LARGE:
                offHeapStorage.put(key, serializer.serialize(value));
                break;
        }
        
        size.incrementAndGet();
    }
    
    public V get(K key) {
        switch (currentType) {
            case SMALL:
                return smallStorage.get(key);
            case MEDIUM:
                return mediumStorage.get(key);
            case LARGE:
                byte[] data = offHeapStorage.get(key);
                return data != null ? serializer.deserialize(data) : null;
            default:
                return null;
        }
    }
    
    private synchronized void checkAndUpgrade() {
        int currentSize = size.get();
        
        if (currentType == StorageType.SMALL && currentSize >= SMALL_THRESHOLD) {
            upgradeToMedium();
        } else if (currentType == StorageType.MEDIUM && currentSize >= MEDIUM_THRESHOLD) {
            upgradeToLarge();
        }
    }
    
    private void upgradeToMedium() {
        mediumStorage = new ShardedStorage<>(Runtime.getRuntime().availableProcessors() * 2);
        smallStorage.forEach(mediumStorage::put);
        smallStorage = null; // 释放内存
        currentType = StorageType.MEDIUM;
    }
    
    private void upgradeToLarge() {
        offHeapStorage = new OffHeapStorage<>(calculateCapacity());
        mediumStorage.forEach((k, v) -> offHeapStorage.put(k, serializer.serialize(v)));
        mediumStorage = null; // 释放内存
        currentType = StorageType.LARGE;
    }
    
    private int calculateCapacity() {
        // 估算所需容量
        return size.get() * 256; // 假设每个条目平均256字节
    }
    
    public void remove(K key) {
        switch (currentType) {
            case SMALL:
                smallStorage.remove(key);
                break;
            case MEDIUM:
                mediumStorage.remove(key);
                break;
            case LARGE:
                offHeapStorage.remove(key);
                break;
        }
        size.decrementAndGet();
    }
    
    public void clear() {
        switch (currentType) {
            case SMALL:
                smallStorage.clear();
                break;
            case MEDIUM:
                mediumStorage.clear();
                break;
            case LARGE:
                offHeapStorage.clear();
                break;
        }
        size.set(0);
    }
    
    public int size() {
        return size.get();
    }
    
    public Set<Map.Entry<K, V>> entrySet() {
        switch (currentType) {
            case SMALL:
                return smallStorage.entrySet();
            case MEDIUM:
                return mediumStorage.entrySet();
            case LARGE:
                // 需要转换堆外存储的数据
                return offHeapStorage.entrySet().stream()
                    .map(e -> new AbstractMap.SimpleEntry<>(
                        e.getKey(), 
                        serializer.deserialize(e.getValue())
                    ))
                    .collect(Collectors.toSet());
            default:
                return Collections.emptySet();
        }
    }
    
    public Set<K> keySet() {
        switch (currentType) {
            case SMALL:
                return smallStorage.keySet();
            case MEDIUM:
                return mediumStorage.keySet();
            case LARGE:
                return offHeapStorage.keySet();
            default:
                return Collections.emptySet();
        }
    }
    
    public interface Serializer<T> {
        byte[] serialize(T value);
        T deserialize(byte[] data);
    }
} 