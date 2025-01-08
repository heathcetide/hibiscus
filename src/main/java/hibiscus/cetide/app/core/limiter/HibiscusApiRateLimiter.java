package hibiscus.cetide.app.core.limiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class HibiscusApiRateLimiter {
    private static final Logger log = LoggerFactory.getLogger(HibiscusApiRateLimiter.class);
    
    private final Map<String, RateLimit> rateLimits = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, AtomicInteger>> requestCounts = new ConcurrentHashMap<>();
    
    public void setRateLimit(String path, int limit, long windowSeconds) {
        rateLimits.put(path, new RateLimit(limit, windowSeconds));
        log.info("Set rate limit for path {}: {} requests per {} seconds", 
                path, limit, windowSeconds);
    }
    
    public boolean tryAcquire(String path) {
        RateLimit rateLimit = rateLimits.get(path);
        if (rateLimit == null) {
            return true;  // 没有限制
        }
        
        long currentWindow = System.currentTimeMillis() / (rateLimit.windowSeconds * 1000);
        Map<Long, AtomicInteger> pathCounts = requestCounts.computeIfAbsent(path, 
            k -> new ConcurrentHashMap<>());
        
        // 清理旧窗口
        pathCounts.keySet().removeIf(window -> window < currentWindow);
        
        AtomicInteger count = pathCounts.computeIfAbsent(currentWindow, k -> new AtomicInteger(0));
        if (count.get() >= rateLimit.limit) {
            log.warn("Rate limit exceeded for path: {}", path);
            return false;
        }
        
        count.incrementAndGet();
        return true;
    }
    
    private static class RateLimit {
        private final int limit;
        private final long windowSeconds;
        
        public RateLimit(int limit, long windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
        }
    }
} 