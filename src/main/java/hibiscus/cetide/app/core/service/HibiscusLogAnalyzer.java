package hibiscus.cetide.app.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HibiscusLogAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(HibiscusLogAnalyzer.class);
    
    // 错误模式的正则表达式
    private static final Pattern ERROR_PATTERN = Pattern.compile("Exception: (.+?)(?=\\s|$)");
    private static final Pattern API_PATTERN = Pattern.compile("API call: ([^ ]+)");
    
    public Map<String, LogStatistics> analyzeLogPatterns(List<HibiscusLogService.LogEntry> logs) {
        Map<String, LogStatistics> statistics = new HashMap<>();
        
        for (HibiscusLogService.LogEntry logEntry : logs) {
            // 分析异常模式
            if ("ERROR".equals(logEntry.getLevel())) {
                String errorPattern = extractErrorPattern(logEntry.getMessage());
                if (errorPattern != null) {
                    statistics.computeIfAbsent(errorPattern, k -> new LogStatistics())
                        .addOccurrence(parseTimestamp(logEntry.getTimestamp()));
                }
            }
            
            // 分析API调用模式
            if (logEntry.getMessage().contains("API call")) {
                String apiPattern = extractApiPattern(logEntry.getMessage());
                if (apiPattern != null) {
                    statistics.computeIfAbsent(apiPattern, k -> new LogStatistics())
                        .addApiCall(parseTimestamp(logEntry.getTimestamp()));
                }
            }
        }
        
        return statistics;
    }
    
    private String extractErrorPattern(String message) {
        Matcher matcher = ERROR_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractApiPattern(String message) {
        Matcher matcher = API_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private long parseTimestamp(String timestamp) {
        try {
            // 假设时间戳格式为："yyyy-MM-dd HH:mm:ss.SSS"
            String[] parts = timestamp.split("[. ]");
            String datePart = parts[0];
            String timePart = parts[1];
            String millisPart = parts[2];
            
            // 简单的时间戳转换逻辑
            return System.currentTimeMillis();  // 实际应用中需要真实的转换逻辑
        } catch (Exception e) {
            log.error("Error parsing timestamp: {}", timestamp, e);
            return System.currentTimeMillis();
        }
    }
    
    public List<LogCorrelation> findCorrelatedEvents(List<HibiscusLogService.LogEntry> logs,
                                                    String targetEvent, 
                                                    long timeWindowMs) {
        List<LogCorrelation> correlations = new ArrayList<>();
        
        for (int i = 0; i < logs.size(); i++) {
            if (logs.get(i).getMessage().contains(targetEvent)) {
                correlations.add(new LogCorrelation(
                    logs.get(i),
                    findContextLogs(logs, i, timeWindowMs)
                ));
            }
        }
        return correlations;
    }
    
    private List<HibiscusLogService.LogEntry> findContextLogs(List<HibiscusLogService.LogEntry> logs,
                                                              int targetIndex,
                                                              long timeWindowMs) {
        List<HibiscusLogService.LogEntry> contextLogs = new ArrayList<>();
        long targetTime = parseTimestamp(logs.get(targetIndex).getTimestamp());
        
        // 向前查找
        for (int i = targetIndex - 1; i >= 0; i--) {
            HibiscusLogService.LogEntry log = logs.get(i);
            if (targetTime - parseTimestamp(log.getTimestamp()) > timeWindowMs) {
                break;
            }
            contextLogs.add(log);
        }
        
        // 向后查找
        for (int i = targetIndex + 1; i < logs.size(); i++) {
            HibiscusLogService.LogEntry log = logs.get(i);
            if (parseTimestamp(log.getTimestamp()) - targetTime > timeWindowMs) {
                break;
            }
            contextLogs.add(log);
        }
        
        return contextLogs;
    }
    
    public static class LogStatistics {
        private final List<Long> occurrences = new ArrayList<>();
        private final Map<String, Integer> contextualPatterns = new HashMap<>();
        
        public void addOccurrence(long timestamp) {
            occurrences.add(timestamp);
        }
        
        public void addApiCall(long timestamp) {
            occurrences.add(timestamp);
        }
        
        public List<Long> getOccurrences() {
            return new ArrayList<>(occurrences);
        }
        
        public Map<String, Integer> getContextualPatterns() {
            return new HashMap<>(contextualPatterns);
        }
        
        public int getOccurrenceCount() {
            return occurrences.size();
        }
        
        public double getAverageTimeBetweenOccurrences() {
            if (occurrences.size() < 2) {
                return 0;
            }
            
            long totalTime = 0;
            for (int i = 1; i < occurrences.size(); i++) {
                totalTime += occurrences.get(i) - occurrences.get(i-1);
            }
            
            return totalTime / (double)(occurrences.size() - 1);
        }
    }
    
    public static class LogCorrelation {
        private final HibiscusLogService.LogEntry targetLog;
        private final List<HibiscusLogService.LogEntry> contextLogs;
        
        public LogCorrelation(HibiscusLogService.LogEntry targetLog,
                              List<HibiscusLogService.LogEntry> contextLogs) {
            this.targetLog = targetLog;
            this.contextLogs = contextLogs;
        }
        
        public HibiscusLogService.LogEntry getTargetLog() {
            return targetLog;
        }
        
        public List<HibiscusLogService.LogEntry> getContextLogs() {
            return new ArrayList<>(contextLogs);
        }
    }
} 