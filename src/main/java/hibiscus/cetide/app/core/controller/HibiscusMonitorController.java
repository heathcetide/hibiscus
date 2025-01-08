package hibiscus.cetide.app.core.controller;

import hibiscus.cetide.app.core.service.HibiscusMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hibiscus/monitor")
public class HibiscusMonitorController {

    @Autowired
    private HibiscusMonitorService monitorService;

    @GetMapping("/jvm")
    public ResponseEntity<?> getJvmMetrics() {
        try {
            return ResponseEntity.ok(monitorService.getJvmMetrics());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/threads")
    public ResponseEntity<?> getThreadMetrics() {
        try {
            return ResponseEntity.ok(monitorService.getThreadMetrics());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/dbpool")
    public ResponseEntity<?> getDbPoolMetrics() {
        try {
            return ResponseEntity.ok(monitorService.getDbPoolMetrics());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api-stats")
    public ResponseEntity<?> getApiMetrics() {
        try {
            return ResponseEntity.ok(monitorService.getApiMetrics());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
} 