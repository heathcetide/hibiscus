package hibiscus.cetide.app.core.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 收集API路径的异常统计信息。
 * 记录每个API路径抛出的异常类型、发生次数、最近的异常信息及其堆栈。
 */
@Component
public class HibiscusExceptionCollector {
    private static final Logger log = LoggerFactory.getLogger(HibiscusExceptionCollector.class);

    private final Map<String, Map<String, ExceptionStats>> exceptionStats = new ConcurrentHashMap<>();

    /**
     * 记录指定API路径的异常信息。
     *
     * @param path API路径
     * @param ex   异常实例
     */
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

    /**
     * 获取指定API路径的异常统计信息。
     *
     * @param path API路径
     * @return 异常统计信息的Map
     */
    public Map<String, ExceptionStats> getExceptionStats(String path) {
        return exceptionStats.getOrDefault(path, new ConcurrentHashMap<>());
    }

    /**
     * 异常统计信息类，记录异常的类型、次数、最近发生时间及相关信息。
     */
    public static class ExceptionStats {
        private final String exceptionType;
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile String lastMessage;
        private volatile String lastStackTrace;
        private volatile long lastOccurrence;

        public ExceptionStats(String exceptionType) {
            this.exceptionType = exceptionType;
        }

        /**
         * 增加异常计数。
         */
        public void incrementCount() {
            count.incrementAndGet();
        }

        /**
         * 更新最近一次异常的信息。
         *
         * @param ex 异常实例
         */
        public void updateLastOccurrence(Throwable ex) {
            this.lastMessage = ex.getMessage();
            this.lastStackTrace = getStackTraceAsString(ex);
            this.lastOccurrence = System.currentTimeMillis();
        }

        /**
         * 将异常堆栈转化为字符串。
         *
         * @param ex 异常实例
         * @return 异常堆栈的字符串表示
         */
        private String getStackTraceAsString(Throwable ex) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : ex.getStackTrace()) {
                sb.append(element.toString()).append("\n");
            }
            return sb.toString();
        }
        public String getExceptionType() { return exceptionType; }
        public int getCount() { return count.get(); }
        public String getLastMessage() { return lastMessage; }
        public String getLastStackTrace() { return lastStackTrace; }
        public long getLastOccurrence() { return lastOccurrence; }
    }
}