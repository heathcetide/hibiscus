package hibiscus.cetide.app.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class HibiscusLogService {
    private static final Logger log = LoggerFactory.getLogger(HibiscusLogService.class);
    
    @Value("${logging.file.path:logs}")
    private String logPath;
    
    public List<LogEntry> getLogs(String level, String keyword, String startDate, String endDate) {
        List<LogEntry> logs = new ArrayList<>();
        try {
            Path logFile = Paths.get(logPath, "app.log");
            if (Files.exists(logFile)) {
                List<String> lines = Files.readAllLines(logFile);
                
                // 处理日期参数
                LocalDateTime start;
                LocalDateTime end;
                
                try {
                    start = (startDate != null && !startDate.trim().isEmpty()) 
                        ? LocalDate.parse(startDate).atStartOfDay() 
                        : LocalDate.now().minusDays(7).atStartOfDay();
                } catch (DateTimeParseException e) {
                    log.warn("Invalid start date format: {}, using default", startDate);
                    start = LocalDate.now().minusDays(7).atStartOfDay();
                }
                
                try {
                    end = (endDate != null && !endDate.trim().isEmpty())
                        ? LocalDate.parse(endDate).plusDays(1).atStartOfDay()
                        : LocalDateTime.now();
                } catch (DateTimeParseException e) {
                    log.warn("Invalid end date format: {}, using default", endDate);
                    end = LocalDateTime.now();
                }
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                
                for (String line : lines) {
                    LogEntry entry = parseLine(line);
                    if (entry != null) {
                        try {
                            LocalDateTime logTime = LocalDateTime.parse(entry.getTimestamp(), formatter);
                            if (logTime.isAfter(start) && logTime.isBefore(end) && 
                                matchesFilter(entry, level, keyword)) {
                                logs.add(entry);
                            }
                        } catch (DateTimeParseException e) {
                            log.warn("Failed to parse log timestamp: {}", entry.getTimestamp());
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error reading log file", e);
        }
        return logs;
    }
    
    private LogEntry parseLine(String line) {
        try {
            // 确保行内容有效
            if (line == null || line.trim().isEmpty()) {
                return null;
            }

            // 使用更健壮的分割方式
            String[] parts = line.split("\\s+", 6);
            if (parts.length >= 6) {
                String timestamp = parts[0] + " " + parts[1];
                // 验证时间戳格式
                try {
                    LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                } catch (Exception e) {
                    log.warn("Invalid timestamp format: {}", timestamp);
                    return null;
                }

                String thread = parts[2].replaceAll("[\\[\\]]", "");
                String level = parts[3];
                String logger = parts[4];
                String message = parts[5];
                
                return new LogEntry(timestamp, thread, level, logger, message);
            }
        } catch (Exception e) {
            log.warn("Failed to parse log line: {}", line, e);
        }
        return null;
    }
    
    private boolean matchesFilter(LogEntry entry, String level, String keyword) {
        boolean matchesLevel = level == null || level.isEmpty() || 
                             entry.getLevel().equalsIgnoreCase(level);
        boolean matchesKeyword = keyword == null || keyword.isEmpty() || 
                               entry.getMessage().toLowerCase().contains(keyword.toLowerCase());
        return matchesLevel && matchesKeyword;
    }
    
    public void archiveLogs() {
        try {
            Path logDir = Paths.get(logPath);
            Path archiveDir = logDir.resolve("archive");
            Files.createDirectories(archiveDir);
            
            // 移动旧日志文件到归档目录
            Files.list(logDir)
                .filter(path -> path.toString().endsWith(".log"))
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        Path target = archiveDir.resolve(path.getFileName());
                        Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
                        log.info("Archived log file: {}", path);
                    } catch (IOException e) {
                        log.error("Failed to archive log file: {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Error archiving logs", e);
        }
    }
    
    public byte[] exportLogs() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            
            Path logDir = Paths.get(logPath);
            Files.walk(logDir)
                .filter(path -> path.toString().endsWith(".log"))
                .forEach(path -> {
                    try {
                        ZipEntry entry = new ZipEntry(logDir.relativize(path).toString());
                        zos.putNextEntry(entry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        log.error("Failed to add log file to zip: {}", path, e);
                    }
                });
            
            zos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error exporting logs", e);
            return new byte[0];
        }
    }
    
    public void compressLogs() {
        try {
            Path logsDir = Paths.get(logPath);
            if (!Files.exists(logsDir)) {
                return;
            }

            // 获取所有需要压缩的日志文件
            List<Path> oldLogs = Files.list(logsDir)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.matches("helper\\.\\d{4}-\\d{2}-\\d{2}\\.log");
                })
                .collect(Collectors.toList());

            for (Path oldLog : oldLogs) {
                Path gzipFile = Paths.get(oldLog.toString() + ".gz");
                
                // 使用try-with-resources确保文件正确关闭
                try (InputStream fis = new BufferedInputStream(Files.newInputStream(oldLog));
                     OutputStream fos = new BufferedOutputStream(Files.newOutputStream(gzipFile));
                     GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                    
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        gzos.write(buffer, 0, len);
                    }
                }
                
                // 确保输出流完全关闭后再删除原文件
                try {
                    Thread.sleep(100); // 给文件系统一些时间完全释放文件
                    Files.deleteIfExists(oldLog);
                    log.info("Compressed log file: {}", oldLog);
                } catch (Exception e) {
                    log.warn("Could not delete original log file: {}. It will be deleted on next attempt.", oldLog);
                }
            }
        } catch (IOException e) {
            log.error("Error compressing logs", e);
            throw new RuntimeException("Failed to compress logs", e);
        }
    }
    
    // 获取可用的日志日期范围
    public Map<String, LocalDate> getAvailableDateRange() {
        Map<String, LocalDate> range = new HashMap<>();
        try {
            Path logFile = Paths.get(logPath, "app.log");
            if (Files.exists(logFile)) {
                List<String> lines = Files.readAllLines(logFile);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                
                LocalDate earliest = null;
                LocalDate latest = null;
                
                for (String line : lines) {
                    LogEntry entry = parseLine(line);
                    if (entry != null && entry.getTimestamp() != null) {
                        try {
                            LocalDateTime logTime = LocalDateTime.parse(entry.getTimestamp(), formatter);
                            LocalDate logDate = logTime.toLocalDate();
                            
                            if (earliest == null || logDate.isBefore(earliest)) {
                                earliest = logDate;
                            }
                            if (latest == null || logDate.isAfter(latest)) {
                                latest = logDate;
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse timestamp: {}", entry.getTimestamp());
                        }
                    }
                }
                
                // 如果没有找到有效日期，使用默认值
                range.put("earliest", earliest != null ? earliest : LocalDate.now().minusDays(7));
                range.put("latest", latest != null ? latest : LocalDate.now());
            }
        } catch (IOException e) {
            log.error("Error getting log date range", e);
            // 发生错误时返回默认范围
            range.put("earliest", LocalDate.now().minusDays(7));
            range.put("latest", LocalDate.now());
        }
        return range;
    }
    
    public static class LogEntry {
        private final String timestamp;
        private final String thread;
        private final String level;
        private final String logger;
        private final String message;
        
        public LogEntry(String timestamp, String thread, String level, 
                       String logger, String message) {
            this.timestamp = timestamp;
            this.thread = thread;
            this.level = level;
            this.logger = logger;
            this.message = message;
        }
        
        public String getTimestamp() { return timestamp; }
        public String getThread() { return thread; }
        public String getLevel() { return level; }
        public String getLogger() { return logger; }
        public String getMessage() { return message; }
    }
} 