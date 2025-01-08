package hibiscus.cetide.app.component.signal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SignalMetrics {
    private final Map<String, AtomicLong> emitCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> handlerCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> processingTime = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCount = new ConcurrentHashMap<>();
    private final Map<String, Long> lastEmitTime = new ConcurrentHashMap<>();

    public void recordEmit(String signalName) {
        emitCount.computeIfAbsent(signalName, k -> new AtomicLong()).incrementAndGet();
        lastEmitTime.put(signalName, System.currentTimeMillis());
    }

    public void recordHandlerAdded(String signalName) {
        handlerCount.computeIfAbsent(signalName, k -> new AtomicLong()).incrementAndGet();
    }

    public void recordHandlerRemoved(String signalName) {
        handlerCount.computeIfAbsent(signalName, k -> new AtomicLong()).decrementAndGet();
    }

    public void recordProcessingTime(String signalName, long timeInMillis) {
        processingTime.computeIfAbsent(signalName, k -> new AtomicLong()).addAndGet(timeInMillis);
    }

    public void recordError(String signalName) {
        errorCount.computeIfAbsent(signalName, k -> new AtomicLong()).incrementAndGet();
    }

    public Map<String, Object> getMetrics(String signalName) {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("emitCount", emitCount.getOrDefault(signalName, new AtomicLong()).get());
        metrics.put("handlerCount", handlerCount.getOrDefault(signalName, new AtomicLong()).get());
        metrics.put("totalProcessingTime", processingTime.getOrDefault(signalName, new AtomicLong()).get());
        metrics.put("errorCount", errorCount.getOrDefault(signalName, new AtomicLong()).get());
        metrics.put("lastEmitTime", lastEmitTime.getOrDefault(signalName, 0L));
        return metrics;
    }

    public Map<String, Map<String, Object>> getAllMetrics() {
        Map<String, Map<String, Object>> allMetrics = new ConcurrentHashMap<>();
        emitCount.keySet().forEach(signalName -> 
            allMetrics.put(signalName, getMetrics(signalName))
        );
        return allMetrics;
    }
} 