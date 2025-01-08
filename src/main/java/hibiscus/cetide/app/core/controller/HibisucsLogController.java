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
    
    @GetMapping("/api/logs")
    @ResponseBody
    public List<HibiscusLogService.LogEntry> getLogs(
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate) {
        return logService.getLogs(level, keyword, startDate, endDate);
    }
    
    @GetMapping("/api/date-range")
    @ResponseBody
    public Map<String, LocalDate> getDateRange() {
        return logService.getAvailableDateRange();
    }
    
    @PostMapping("/api/logs/archive")
    @ResponseBody
    public ResponseEntity<Void> archiveLogs() {
        logService.archiveLogs();
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/api/logs/compress")
    @ResponseBody
    public ResponseEntity<Void> compressLogs() {
        logService.compressLogs();
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/api/logs/export")
    public ResponseEntity<byte[]> exportLogs() {
        byte[] logs = logService.exportLogs();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=logs.zip")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(logs);
    }
} 