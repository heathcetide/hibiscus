package hibiscus.cetide.app.component.cache.pool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.Queue;
import java.util.Map;

public class ObjectPoolManager<T> {
    private final ConcurrentHashMap<Class<?>, ObjectPool<T>> pools;
    private final int maxPoolSize;
    private final PoolMetrics metrics;
    private final ScheduledExecutorService maintenance;
    
    private static class ObjectPool<T> {
        private final Queue<T> objects;
        private final Supplier<T> factory;
        private final int maxSize;
        private final AtomicInteger created;
        private final AtomicInteger borrowed;
        
        public ObjectPool(Supplier<T> factory, int maxSize) {
            this.objects = new ConcurrentLinkedQueue<>();
            this.factory = factory;
            this.maxSize = maxSize;
            this.created = new AtomicInteger();
            this.borrowed = new AtomicInteger();
        }
        
        public T acquire() {
            T obj = objects.poll();
            if (obj == null) {
                if (created.get() < maxSize) {
                    obj = factory.get();
                    created.incrementAndGet();
                } else {
                    // 等待对象返回池中
                    while ((obj = objects.poll()) == null) {
                        Thread.yield();
                    }
                }
            }
            borrowed.incrementAndGet();
            return obj;
        }
        
        public void release(T obj) {
            objects.offer(obj);
            borrowed.decrementAndGet();
        }
        
        public int getSize() {
            return objects.size();
        }
        
        public int getBorrowed() {
            return borrowed.get();
        }
    }
    
    public ObjectPoolManager(int maxPoolSize) {
        this.pools = new ConcurrentHashMap<>();
        this.maxPoolSize = maxPoolSize;
        this.metrics = new PoolMetrics();
        this.maintenance = Executors.newScheduledThreadPool(1);
        
        startMaintenanceTask();
    }
    
    private void startMaintenanceTask() {
        maintenance.scheduleAtFixedRate(() -> {
            try {
                performMaintenance();
            } catch (Exception e) {
                // 记录错误但继续运行
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    private void performMaintenance() {
        pools.forEach((type, pool) -> {
            int size = pool.getSize();
            int borrowed = pool.getBorrowed();
            
            metrics.recordPoolStats(type.getName(), size, borrowed);
            
            // 如果池太大且使用率低，减少大小
            if (size > maxPoolSize / 2 && borrowed < size / 4) {
                shrinkPool(pool);
            }
        });
    }
    
    private void shrinkPool(ObjectPool<T> pool) {
        int targetSize = Math.max(maxPoolSize / 4, pool.getBorrowed() * 2);
        while (pool.getSize() > targetSize) {
            pool.objects.poll();
        }
    }
    
    public <R extends T> void registerPool(Class<R> type, Supplier<R> factory) {
        pools.putIfAbsent(type, new ObjectPool<>((Supplier<T>) factory, maxPoolSize));
    }
    
    @SuppressWarnings("unchecked")
    public <R extends T> R acquire(Class<R> type) {
        ObjectPool<T> pool = pools.get(type);
        if (pool == null) {
            throw new IllegalArgumentException("No pool registered for type: " + type);
        }
        
        long startTime = System.nanoTime();
        T obj = pool.acquire();
        metrics.recordAcquisition(System.nanoTime() - startTime);
        
        return (R) obj;
    }
    
    public <R extends T> void release(R obj) {
        ObjectPool<T> pool = pools.get(obj.getClass());
        if (pool != null) {
            pool.release((T) obj);
        }
    }
    
    public PoolMetrics getMetrics() {
        return metrics;
    }
    
    public void shutdown() {
        maintenance.shutdown();
        pools.clear();
    }
} 