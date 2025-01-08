package hibiscus.cetide.app.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class HibiscusPerformanceAnalyzer {
    
    private static final Logger log = LoggerFactory.getLogger(HibiscusPerformanceAnalyzer.class);
    private final Map<String, ApiMetrics> metricsMap = new ConcurrentHashMap<>();
    
    public void recordApiCall(String path, long duration, int statusCode) {
        ApiMetrics metrics = metricsMap.computeIfAbsent(path, k -> new ApiMetrics());
        metrics.recordCall(duration, statusCode);
        
        log.debug("Recorded API call: path={}, duration={}ms, status={}", 
            path, duration, statusCode);
    }
    
    public Map<String, ApiMetrics> getMetrics() {
        return metricsMap;
    }
    
    public static class ApiMetrics {
        private final AtomicLong totalCalls = new AtomicLong();
        private final AtomicLong totalDuration = new AtomicLong();
        private final AtomicLong maxDuration = new AtomicLong();
        private final AtomicLong successCalls = new AtomicLong();
        private final AtomicLong failedCalls = new AtomicLong();
        
        public void recordCall(long duration, int statusCode) {
            totalCalls.incrementAndGet();
            totalDuration.addAndGet(duration);
            maxDuration.updateAndGet(current -> Math.max(current, duration));
            
            if (statusCode >= 200 && statusCode < 300) {
                successCalls.incrementAndGet();
            } else {
                failedCalls.incrementAndGet();
            }
        }
        
        // Getters
        public long getTotalCalls() { return totalCalls.get(); }
        public long getTotalDuration() { return totalDuration.get(); }
        public long getMaxDuration() { return maxDuration.get(); }
        public long getSuccessCalls() { return successCalls.get(); }
        public long getFailedCalls() { return failedCalls.get(); }
    }
} 