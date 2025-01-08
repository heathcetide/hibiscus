package hibiscus.cetide.app.component.cache.adaptive;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

public class AdaptiveStrategy {
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicInteger currentSize = new AtomicInteger();
    private final AtomicLong lastResizeTime = new AtomicLong();
    
    private static final double TARGET_HIT_RATE = 0.85;
    private static final long RESIZE_INTERVAL = TimeUnit.MINUTES.toMillis(5);
    private static final double GROWTH_FACTOR = 1.5;
    private static final double SHRINK_FACTOR = 0.75;
    
    public void recordHit() {
        hitCount.incrementAndGet();
        maybeResize();
    }
    
    public void recordMiss() {
        missCount.incrementAndGet();
        maybeResize();
    }
    
    private void maybeResize() {
        long now = System.currentTimeMillis();
        long lastResize = lastResizeTime.get();
        
        if (now - lastResize > RESIZE_INTERVAL && 
            lastResizeTime.compareAndSet(lastResize, now)) {
            resize();
        }
    }
    
    private void resize() {
        long hits = hitCount.get();
        long total = hits + missCount.get();
        if (total == 0) return;
        
        double hitRate = (double) hits / total;
        int size = currentSize.get();
        
        if (hitRate < TARGET_HIT_RATE) {
            // 命中率低，增加缓存大小
            currentSize.set((int)(size * GROWTH_FACTOR));
        } else if (hitRate > TARGET_HIT_RATE + 0.1) {
            // 命中率高，可以适当减小缓存
            currentSize.set((int)(size * SHRINK_FACTOR));
        }
        
        // 重置计数器
        hitCount.set(0);
        missCount.set(0);
    }
    
    public int getOptimalSize() {
        return currentSize.get();
    }
} 