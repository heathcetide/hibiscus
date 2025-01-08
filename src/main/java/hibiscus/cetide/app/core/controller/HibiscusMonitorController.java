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

    /**
     * 获取JVM性能指标信息。
     *
     * @return 返回JVM性能指标数据或错误信息。
     */
    @GetMapping("/jvm")
    public ResponseEntity<?> getJvmMetrics() {
        try {
            return ResponseEntity.ok(monitorService.getJvmMetrics());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取线程使用情况的监控数据。
     *
     * @return 返回线程使用指标数据或错误信息。
     */
    @GetMapping("/threads")
    public ResponseEntity<?> getThreadMetrics() {
        try {
            return ResponseEntity.ok(monitorService.getThreadMetrics());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取数据库连接池的监控数据。
     *
     * @return 返回数据库连接池指标数据或错误信息。
     */
    @GetMapping("/dbpool")
    public ResponseEntity<?> getDbPoolMetrics() {
        try {
            return ResponseEntity.ok(monitorService.getDbPoolMetrics());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取API性能统计数据。
     *
     * @return 返回API性能指标数据或错误信息。
     */
    @GetMapping("/api-stats")
    public ResponseEntity<?> getApiMetrics() {
        try {
            return ResponseEntity.ok(monitorService.getApiMetrics());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
} 