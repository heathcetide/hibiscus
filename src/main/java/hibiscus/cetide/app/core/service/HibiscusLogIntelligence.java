package hibiscus.cetide.app.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class HibiscusLogIntelligence {
    private static final Logger log = LoggerFactory.getLogger(HibiscusLogIntelligence.class);
    
    // 错误模式的正则表达式
    private static final Map<String, Pattern> ERROR_PATTERNS = new HashMap<>();
    static {
        ERROR_PATTERNS.put("NullPointer", Pattern.compile("NullPointerException"));
        ERROR_PATTERNS.put("SQLException", Pattern.compile("SQLException"));
        ERROR_PATTERNS.put("IOException", Pattern.compile("IOException"));
        // 添加更多错误模式
    }
    
    public List<AnomalyDetection> detectAnomalies(List<HibiscusLogService.LogEntry> logs) {
        List<AnomalyDetection> anomalies = new ArrayList<>();
        
        detectErrorSpikes(logs, anomalies);
        detectPerformanceAnomalies(logs, anomalies);
        detectAnomalousPatterns(logs, anomalies);
        
        return anomalies;
    }
    
    private void detectErrorSpikes(List<HibiscusLogService.LogEntry> logs, List<AnomalyDetection> anomalies) {
        Map<String, Integer> errorCounts = new HashMap<>();
        Map<String, Double> baselineErrors = new HashMap<>();
        
        // 统计错误次数
        for (HibiscusLogService.LogEntry log : logs) {
            if ("ERROR".equals(log.getLevel())) {
                String errorType = extractErrorType(log.getMessage());
                errorCounts.merge(errorType, 1, Integer::sum);
            }
        }
        
        // 检测异常spike
        errorCounts.forEach((errorType, count) -> {
            double baseline = baselineErrors.getOrDefault(errorType, 10.0);
            if (count > baseline * 2) {  // 如果错误数量超过基线的2倍
                anomalies.add(new AnomalyDetection(
                    AnomalyType.ERROR_SPIKE,
                    "Error spike detected for " + errorType,
                    count,
                    baseline
                ));
            }
        });
    }
    
    private void detectPerformanceAnomalies(List<HibiscusLogService.LogEntry> logs, List<AnomalyDetection> anomalies) {
        // 提取性能相关的日志
        List<HibiscusLogService.LogEntry> perfLogs = logs.stream()
            .filter(log -> log.getMessage().contains("duration="))
            .collect(Collectors.toList());
            
        // 计算平均响应时间
        if (!perfLogs.isEmpty()) {
            double avgDuration = calculateAverageDuration(perfLogs);
            double threshold = avgDuration * 2;  // 设置阈值为平均值的2倍
            
            // 检测超时请求
            for (HibiscusLogService.LogEntry log : perfLogs) {
                long duration = extractDuration(log.getMessage());
                if (duration > threshold) {
                    anomalies.add(new AnomalyDetection(
                        AnomalyType.PERFORMANCE_DEGRADATION,
                        "Slow request detected",
                        duration,
                        avgDuration
                    ));
                }
            }
        }
    }
    
    private void detectAnomalousPatterns(List<HibiscusLogService.LogEntry> logs, List<AnomalyDetection> anomalies) {
        // 实现异常模式检测逻辑
        Map<String, Integer> patternCounts = new HashMap<>();
        
        for (HibiscusLogService.LogEntry log : logs) {
            String pattern = extractLogPattern(log.getMessage());
            patternCounts.merge(pattern, 1, Integer::sum);
        }
        
        // 检测异常模式
        patternCounts.forEach((pattern, count) -> {
            if (isAnomalousPattern(pattern, count)) {
                anomalies.add(new AnomalyDetection(
                    AnomalyType.UNUSUAL_PATTERN,
                    "Unusual log pattern detected: " + pattern,
                    count,
                    0
                ));
            }
        });
    }
    
    public Map<String, List<String>> suggestSolutions(List<HibiscusLogService.LogEntry> logs) {
        Map<String, List<String>> solutions = new HashMap<>();
        
        for (HibiscusLogService.LogEntry log : logs) {
            if ("ERROR".equals(log.getLevel())) {
                String errorType = extractErrorType(log.getMessage());
                solutions.put(errorType, findSimilarCases(errorType));
            }
        }
        
        return solutions;
    }
    
    private String extractErrorType(String message) {
        for (Map.Entry<String, Pattern> entry : ERROR_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(message).find()) {
                return entry.getKey();
            }
        }
        return "Unknown";
    }
    
    private List<String> findSimilarCases(String errorType) {
        // 这里可以实现查询知识库或历史案例的逻辑
        List<String> suggestions = new ArrayList<>();
        suggestions.add("Check related logs for more context");
        suggestions.add("Review recent code changes");
        suggestions.add("Check system resources");
        return suggestions;
    }
    
    private String extractLogPattern(String message) {
        // 实现日志模式提取逻辑
        return message.replaceAll("\\d+", "N")
                     .replaceAll("\\b[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}\\b", "EMAIL")
                     .replaceAll("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b", "IP");
    }
    
    private boolean isAnomalousPattern(String pattern, int count) {
        // 实现异常模式判断逻辑
        return count > 100;  // 示例阈值
    }
    
    private double calculateAverageDuration(List<HibiscusLogService.LogEntry> logs) {
        return logs.stream()
            .mapToLong(log -> extractDuration(log.getMessage()))
            .average()
            .orElse(0);
    }
    
    private long extractDuration(String message) {
        // 从日志消息中提取持续时间
        try {
            int start = message.indexOf("duration=") + 9;
            int end = message.indexOf("ms", start);
            return Long.parseLong(message.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }
    
    public enum AnomalyType {
        ERROR_SPIKE,
        PERFORMANCE_DEGRADATION,
        UNUSUAL_PATTERN
    }
    
    public static class AnomalyDetection {
        private final AnomalyType type;
        private final String description;
        private final double actualValue;
        private final double expectedValue;
        
        public AnomalyDetection(AnomalyType type, String description, 
                               double actualValue, double expectedValue) {
            this.type = type;
            this.description = description;
            this.actualValue = actualValue;
            this.expectedValue = expectedValue;
        }
        
        public AnomalyType getType() { return type; }
        public String getDescription() { return description; }
        public double getActualValue() { return actualValue; }
        public double getExpectedValue() { return expectedValue; }
    }
}

/**
 * 高级搜索功能：
 * 时间范围过滤
 * 异常类型过滤
 * API端点过滤
 * 自定义搜索模式
 * 日志分析：
 * 错误模式识别
 * 相关事件关联
 * 统计分析
 * 可视化：
 * 时间轴视图
 * 错误分布热图
 * 实时监控面板
 * 智能分析：
 * 异常检测
 * 解决方案推荐
 * 模式识别
 * 上下文分析：
 * 相关日志关联
 * 事件链追踪
 * 根因分析
 */