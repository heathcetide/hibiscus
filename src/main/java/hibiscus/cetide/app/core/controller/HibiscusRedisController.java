package hibiscus.cetide.app.core.controller;

import hibiscus.cetide.app.core.service.HibiscusRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hibiscus/redis")
public class HibiscusRedisController {
    @Autowired
    private HibiscusRedisService redisService;

    @PostMapping("/connect")
    public ResponseEntity<Map<String, Object>> connect(@RequestBody Map<String, Object> config) {
        Map<String, Object> response = new HashMap<>();
        try {
            String host = (String) config.get("host");
            int port = Integer.parseInt(config.get("port").toString());
            String password = (String) config.get("password");
            int database = Integer.parseInt(config.get("database").toString());

            redisService.connect(host, port, password, database);
            response.put("success", true);
            response.put("message", "连接成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "连接失败: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/keys")
    public ResponseEntity<Map<String, Object>> getKeys(@RequestParam(defaultValue = "*") String pattern) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> keys = redisService.getKeys(pattern);
            response.put("success", true);
            response.put("keys", keys);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取键列表失败: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/key/{key}")
    public ResponseEntity<Map<String, Object>> getKey(@PathVariable String key) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> keyInfo = redisService.getKey(key);
            if (keyInfo != null) {
                response.put("success", true);
                response.putAll(keyInfo);
            } else {
                response.put("success", false);
                response.put("message", "键不存在");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取键值失败: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/key")
    public ResponseEntity<Map<String, Object>> addKey(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String key = (String) request.get("key");
            String type = (String) request.get("type");
            Object value = request.get("value");

            redisService.setKey(key, type, value);
            response.put("success", true);
            response.put("message", "添加成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "添加失败: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/key/{key}")
    public ResponseEntity<Map<String, Object>> updateKey(
            @PathVariable String key,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String type = (String) request.get("type");
            Object value = request.get("value");

            redisService.setKey(key, type, value);
            response.put("success", true);
            response.put("message", "更新成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/key/{key}")
    public ResponseEntity<Map<String, Object>> deleteKey(@PathVariable String key) {
        Map<String, Object> response = new HashMap<>();
        try {
            redisService.deleteKey(key);
            response.put("success", true);
            response.put("message", "删除成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/key/{key}/ttl")
    public ResponseEntity<Map<String, Object>> setExpire(
            @PathVariable String key,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            long ttl = Long.parseLong(request.get("ttl").toString());
            redisService.setExpire(key, ttl);
            response.put("success", true);
            response.put("message", "设置成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "设置失败: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/connection-info")
    public ResponseEntity<Map<String, Object>> getConnectionInfo() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> info = redisService.getConnectionInfo();
            response.put("success", true);
            response.putAll(info);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取连接信息失败: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
} 