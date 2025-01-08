package hibiscus.cetide.app.config;

import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.stats.CacheStats;

import java.util.Map;
import java.util.Set;

public class BusinessCache<K, V> {
    private final HibiscusCache<Object, Object> delegate;

    public BusinessCache(HibiscusCache<Object, Object> delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings("unchecked")
    public V get(K key) {
        return (V) delegate.get(key);
    }

    public void put(K key, V value) {
        delegate.put(key, value);
    }

    public void remove(K key) {
        delegate.remove(key);
    }

    // 添加批量操作方法
    public void putAll(Map<? extends K, ? extends V> map) {
        map.forEach(this::put);
    }

    @SuppressWarnings("unchecked")
    public Map<K, V> getAll() {
        return (Map<K, V>) delegate.getAll();
    }

    // 添加查询方法
    public boolean containsKey(K key) {
        return delegate.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public Set<K> keys() {
        return (Set<K>) delegate.getKeys();
    }


    // 添加统计方法
    public long size() {
        return delegate.size();
    }


    // 添加清理方法
    public void clear() {
        delegate.clear();
    }

    // 添加统计信息获取方法
    public CacheStats stats() {
        return delegate.getStats();
    }

    // 内部接口定义
    public interface EvictionListener<K, V> {
        void onEviction(K key, V value);
    }

    // 获取底层缓存实例
    public HibiscusCache<Object, Object> getDelegate() {
        return delegate;
    }
}