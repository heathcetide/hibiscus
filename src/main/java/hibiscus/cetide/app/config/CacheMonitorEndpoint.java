package hibiscus.cetide.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hibiscus.cetide.app.component.cache.monitor.CacheMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RestController
@RequestMapping("/api/monitor/cache")
public class CacheMonitorEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(CacheMonitorEndpoint.class);
    private final Map<String, CacheMonitorService<Object, Object>> monitorServices;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CacheMonitorEndpoint(Map<String, CacheMonitorService<Object, Object>> monitorServices) {
        this.monitorServices = monitorServices;
    }

    @GetMapping("/{cacheName}/health")
    public Map<String, Object> getHealthStatus(@PathVariable String cacheName) {
        try {
            CacheMonitorService<Object, Object> monitorService = getMonitorService(cacheName);
            Map<String, Object> result = new LinkedHashMap<>();
            
            // 基本统计信息
            result.put("status", "UP");
            result.put("size", monitorService.getSize());
            result.put("hitRate", String.format("%.2f%%", monitorService.getHitRate() * 100));
            result.put("hitCount", monitorService.getHitCount());
            result.put("missCount", monitorService.getMissCount());
            result.put("evictionCount", monitorService.getEvictionCount());
            
            // 内存使用情况
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            Map<String, Object> memory = new LinkedHashMap<>();
            memory.put("total", String.format("%.2f MB", totalMemory / (1024.0 * 1024.0)));
            memory.put("used", String.format("%.2f MB", usedMemory / (1024.0 * 1024.0)));
            memory.put("free", String.format("%.2f MB", freeMemory / (1024.0 * 1024.0)));
            result.put("memory", memory);

            return result;
        } catch (Exception e) {
            logger.error("获取缓存状态时发生错误", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "DOWN");
            error.put("error", "获取缓存状态失败: " + e.getMessage());
            return error;
        }
    }

    @GetMapping("/{cacheName}/stats")
    public Map<String, Object> getDetailedStats(@PathVariable String cacheName) {
        try {
            CacheMonitorService<Object, Object> monitorService = getMonitorService(cacheName);
            Map<String, Object> stats = new LinkedHashMap<>();
            
            // 缓存统计
            Map<String, Object> cacheStats = new LinkedHashMap<>();
            cacheStats.put("size", monitorService.getSize());
            cacheStats.put("hitCount", monitorService.getHitCount());
            cacheStats.put("missCount", monitorService.getMissCount());
            cacheStats.put("hitRate", String.format("%.2f%%", monitorService.getHitRate() * 100));
            cacheStats.put("evictionCount", monitorService.getEvictionCount());
            stats.put("cacheStats", cacheStats);

            return stats;
        } catch (Exception e) {
            logger.error("获取详细统计信息时发生错误", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取统计信息失败: " + e.getMessage());
            return error;
        }
    }

    @GetMapping("/list")
    public Map<String, Object> listCaches() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("caches", monitorServices.keySet());
        return result;
    }

    private CacheMonitorService<Object, Object> getMonitorService(String cacheName) {
        CacheMonitorService<Object, Object> service = monitorServices.get(cacheName);
        if (service == null) {
            throw new IllegalArgumentException("Cache not found: " + cacheName);
        }
        return service;
    }

    @PostConstruct
    public void init() {
        logger.info("CacheMonitorEndpoint initialized with {} cache(s)", monitorServices.size());
    }
}