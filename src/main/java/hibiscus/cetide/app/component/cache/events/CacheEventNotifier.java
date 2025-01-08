package hibiscus.cetide.app.component.cache.events;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CacheEventNotifier<K, V> {
    private final List<Consumer<CacheEvent<K, V>>> listeners = new CopyOnWriteArrayList<>();

    public void addListener(Consumer<CacheEvent<K, V>> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<CacheEvent<K, V>> listener) {
        listeners.remove(listener);
    }

    public void notifyEvent(CacheEvent<K, V> event) {
        for (Consumer<CacheEvent<K, V>> listener : listeners) {
            listener.accept(event);
        }
    }

    public static class CacheEvent<K, V> {
        private final K key;
        private final V value;
        private final EventType type;

        public CacheEvent(K key, V value, EventType type) {
            this.key = key;
            this.value = value;
            this.type = type;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public EventType getType() {
            return type;
        }
    }

    public enum EventType {
        PUT, GET, REMOVE, EVICT
    }
} 