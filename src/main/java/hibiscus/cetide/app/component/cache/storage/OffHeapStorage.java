package hibiscus.cetide.app.component.cache.storage;

import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OffHeapStorage<K> {
    private final ByteBuffer buffer;
    private final Map<K, Integer> index;
    
    public OffHeapStorage(int capacity) {
        this.buffer = ByteBuffer.allocateDirect(capacity);
        this.index = new ConcurrentHashMap<>();
    }
    
    public void put(K key, byte[] value) {
        int position = buffer.position();
        buffer.putInt(value.length);
        buffer.put(value);
        index.put(key, position);
    }
    
    public byte[] get(K key) {
        Integer position = index.get(key);
        if (position == null) return null;
        
        buffer.position(position);
        int length = buffer.getInt();
        byte[] value = new byte[length];
        buffer.get(value);
        return value;
    }

    public void remove(K key) {
        index.remove(key);
    }

    public void clear() {
        index.clear();
        buffer.clear();
    }

    public Set<Map.Entry<K, byte[]>> entrySet() {
        return index.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), get(e.getKey())))
            .collect(Collectors.toSet());
    }

    public Set<K> keySet() {
        return index.keySet();
    }
} 