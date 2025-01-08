package hibiscus.cetide.app.component.cache.cache;

import java.util.concurrent.TimeUnit;
import java.io.Serializable;

public class CacheEntry<V> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final V value;
    private final long expireTime;

    public CacheEntry(V value, long ttl) {
        this.value = value;
        this.expireTime = ttl <= 0 ? -1 : System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(ttl);
    }

    public V getValue() {
        return value;
    }

    public boolean isExpired() {
        return expireTime > 0 && System.nanoTime() >= expireTime;
    }
} 