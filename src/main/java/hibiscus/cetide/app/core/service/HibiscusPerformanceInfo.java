package hibiscus.cetide.app.core.service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class HibiscusPerformanceInfo {
    private final AtomicInteger totalCalls = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicInteger successCalls = new AtomicInteger(0);
    private final AtomicInteger failedCalls = new AtomicInteger(0);
    private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxResponseTime = new AtomicLong(0);
    
    public void recordCall(long duration, int statusCode) {
        totalCalls.incrementAndGet();
        totalResponseTime.addAndGet(duration);
        
        if (statusCode >= 200 && statusCode < 300) {
            successCalls.incrementAndGet();
        } else {
            failedCalls.incrementAndGet();
        }
        
        updateMinMaxResponseTime(duration);
    }
    
    private void updateMinMaxResponseTime(long duration) {
        long currentMin;
        do {
            currentMin = minResponseTime.get();
            if (duration >= currentMin) break;
        } while (!minResponseTime.compareAndSet(currentMin, duration));
        
        long currentMax;
        do {
            currentMax = maxResponseTime.get();
            if (duration <= currentMax) break;
        } while (!maxResponseTime.compareAndSet(currentMax, duration));
    }
    
    public int getTotalCalls() { return totalCalls.get(); }
    public long getAverageResponseTime() { 
        return totalCalls.get() > 0 ? totalResponseTime.get() / totalCalls.get() : 0; 
    }
    public int getSuccessCalls() { return successCalls.get(); }
    public int getFailedCalls() { return failedCalls.get(); }
    public long getMinResponseTime() { return minResponseTime.get(); }
    public long getMaxResponseTime() { return maxResponseTime.get(); }
} 