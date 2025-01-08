package hibiscus.cetide.app.component.cache.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class MetricsCollector {
    private static final MetricsCollector INSTANCE = new MetricsCollector();
    
    private final Map<String, CommandMetrics> commandMetrics = new ConcurrentHashMap<>();
    private final LongAdder totalConnections = new LongAdder();
    private final LongAdder activeConnections = new LongAdder();
    private final LongAdder totalCommands = new LongAdder();
    private final AtomicLong lastSaveTime = new AtomicLong();
    private final Map<String, LongAdder> errorMetrics = new ConcurrentHashMap<>();

    public static class CommandMetrics {
        private final LongAdder calls = new LongAdder();
        private final LongAdder totalTime = new LongAdder();
        private final AtomicLong maxTime = new AtomicLong();
        private final LongAdder failures = new LongAdder();

        public void recordExecution(long executionTime, boolean success) {
            calls.increment();
            if (success) {
                totalTime.add(executionTime);
                updateMaxTime(executionTime);
            } else {
                failures.increment();
            }
        }

        private void updateMaxTime(long executionTime) {
            long current;
            while (executionTime > (current = maxTime.get())) {
                maxTime.compareAndSet(current, executionTime);
            }
        }

        public long getAvgTime() {
            long total = totalTime.sum();
            long count = calls.sum();
            return count == 0 ? 0 : total / count;
        }
    }

    private MetricsCollector() {}

    public static MetricsCollector getInstance() {
        return INSTANCE;
    }

    public void recordCommand(String command, long startTime, boolean success) {
        commandMetrics.computeIfAbsent(command, k -> new CommandMetrics())
                     .recordExecution(System.nanoTime() - startTime, success);
        totalCommands.increment();
    }

    public void recordError(String errorType) {
        errorMetrics.computeIfAbsent(errorType, k -> new LongAdder()).increment();
    }

    public void connectionOpened() {
        totalConnections.increment();
        activeConnections.increment();
    }

    public void connectionClosed() {
        activeConnections.decrement();
    }

    public void recordSave() {
        lastSaveTime.set(System.currentTimeMillis());
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_connections", totalConnections.sum());
        stats.put("active_connections", activeConnections.sum());
        stats.put("total_commands", totalCommands.sum());
        stats.put("last_save_time", lastSaveTime.get());
        
        Map<String, Map<String, Long>> cmdStats = new HashMap<>();
        commandMetrics.forEach((cmd, metrics) -> {
            Map<String, Long> cmdMetrics = new HashMap<>();
            cmdMetrics.put("calls", metrics.calls.sum());
            cmdMetrics.put("avg_time", metrics.getAvgTime());
            cmdMetrics.put("max_time", metrics.maxTime.get());
            cmdMetrics.put("failures", metrics.failures.sum());
            cmdStats.put(cmd, cmdMetrics);
        });
        stats.put("command_stats", cmdStats);
        
        Map<String, Long> errors = new HashMap<>();
        errorMetrics.forEach((type, count) -> errors.put(type, count.sum()));
        stats.put("errors", errors);
        
        return stats;
    }

    public void recordOperation(String operationType) {
        // 实现操作记录逻辑
        long startTime = System.nanoTime();
        // ... 记录操作统计信息
    }
} 