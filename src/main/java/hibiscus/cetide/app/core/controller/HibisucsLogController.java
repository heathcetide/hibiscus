package hibiscus.cetide.app.core.controller;

import hibiscus.cetide.app.core.service.HibiscusLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@Controller
@RequestMapping("/api/hibiscus/logs")
public class HibisucsLogController {
    
    @Autowired
    private HibiscusLogService logService;
    
    @GetMapping
    public String logPage() {
        return "logger/index";
    }

    /**
     * 获取符合条件的日志列表。
     *
     * @param level     日志级别（可选）
     * @param keyword   日志内容关键字（可选）
     * @param startDate 起始日期（可选）
     * @param endDate   结束日期（可选）
     * @return 符合条件的日志列表
     */
    @GetMapping("/api/logs")
    @ResponseBody
    public List<HibiscusLogService.LogEntry> getLogs(
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate) {
        return logService.getLogs(level, keyword, startDate, endDate);
    }

    /**
     * 获取日志的可用日期范围。
     *
     * @return 包含起始日期和结束日期的Map
     */
    @GetMapping("/api/date-range")
    @ResponseBody
    public Map<String, LocalDate> getDateRange() {
        return logService.getAvailableDateRange();
    }

    /**
     * 归档当前的日志文件。
     *
     * @return 空响应，表示归档成功
     */
    @PostMapping("/api/logs/archive")
    @ResponseBody
    public ResponseEntity<Void> archiveLogs() {
        logService.archiveLogs();
        return ResponseEntity.ok().build();
    }

    /**
     * 压缩当前的日志文件。
     *
     * @return 空响应，表示压缩成功
     */
    @PostMapping("/api/logs/compress")
    @ResponseBody
    public ResponseEntity<Void> compressLogs() {
        logService.compressLogs();
        return ResponseEntity.ok().build();
    }

    /**
     * 导出日志文件为压缩包。
     *
     * @return 包含日志文件的响应（ZIP格式）
     */
    @GetMapping("/api/logs/export")
    public ResponseEntity<byte[]> exportLogs() {
        byte[] logs = logService.exportLogs();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=logs.zip")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(logs);
    }
} 