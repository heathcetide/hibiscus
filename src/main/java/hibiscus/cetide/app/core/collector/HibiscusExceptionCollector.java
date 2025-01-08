package hibiscus.cetide.app.core.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class HibiscusExceptionCollector {
    private static final Logger log = LoggerFactory.getLogger(HibiscusExceptionCollector.class);
    
    private final Map<String, Map<String, ExceptionStats>> exceptionStats = new ConcurrentHashMap<>();
    
    public void recordException(String path, Throwable ex) {
        String exceptionType = ex.getClass().getName();
        Map<String, ExceptionStats> pathStats = exceptionStats.computeIfAbsent(path, 
            k -> new ConcurrentHashMap<>());
        
        ExceptionStats stats = pathStats.computeIfAbsent(exceptionType, 
            k -> new ExceptionStats(exceptionType));
        
        stats.incrementCount();
        stats.updateLastOccurrence(ex);
        log.debug("Recorded exception for path: {}, type: {}", path, exceptionType);
    }
    
    public Map<String, ExceptionStats> getExceptionStats(String path) {
        return exceptionStats.getOrDefault(path, new ConcurrentHashMap<>());
    }
    
    public static class ExceptionStats {
        private final String exceptionType;
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile String lastMessage;
        private volatile String lastStackTrace;
        private volatile long lastOccurrence;
        
        public ExceptionStats(String exceptionType) {
            this.exceptionType = exceptionType;
        }
        
        public void incrementCount() {
            count.incrementAndGet();
        }
        
        public void updateLastOccurrence(Throwable ex) {
            this.lastMessage = ex.getMessage();
            this.lastStackTrace = getStackTraceAsString(ex);
            this.lastOccurrence = System.currentTimeMillis();
        }
        
        private String getStackTraceAsString(Throwable ex) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : ex.getStackTrace()) {
                sb.append(element.toString()).append("\n");
            }
            return sb.toString();
        }
        
        // Getters
        public String getExceptionType() { return exceptionType; }
        public int getCount() { return count.get(); }
        public String getLastMessage() { return lastMessage; }
        public String getLastStackTrace() { return lastStackTrace; }
        public long getLastOccurrence() { return lastOccurrence; }
    }
} 