package hibiscus.cetide.app.component.cache.snapshot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class CacheSnapshot<K, V> {
    private final AtomicReference<Map<K, V>> snapshot;
    private final Map<K, V> writeBuffer;
    private volatile long version;
    
    public CacheSnapshot() {
        this.snapshot = new AtomicReference<>(new ConcurrentHashMap<>());
        this.writeBuffer = new ConcurrentHashMap<>();
        this.version = 0;
    }
    
    public V get(K key) {
        V value = writeBuffer.get(key);
        if (value != null) {
            return value;
        }
        return snapshot.get().get(key);
    }
    
    public void put(K key, V value) {
        writeBuffer.put(key, value);
        if (writeBuffer.size() > 1000) { // 配置阈值
            merge();
        }
    }
    
    public synchronized void merge() {
        if (writeBuffer.isEmpty()) return;
        
        Map<K, V> current = snapshot.get();
        Map<K, V> newSnapshot = new ConcurrentHashMap<>(current);
        newSnapshot.putAll(writeBuffer);
        
        snapshot.set(newSnapshot);
        writeBuffer.clear();
        version++;
    }
    
    public long getVersion() {
        return version;
    }
} 