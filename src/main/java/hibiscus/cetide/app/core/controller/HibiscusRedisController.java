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

    /**
     * 连接 Redis 数据库。
     *
     * @param config 包含主机地址、端口、密码和数据库索引的连接配置信息
     * @return 成功或失败信息
     */
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

    /**
     * 获取匹配指定模式的键列表。
     *
     * @param pattern 键匹配模式（默认为 "*"）
     * @return 键列表或错误信息
     */
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

    /**
     * 获取指定键的详细信息。
     *
     * @param key 要查询的键
     * @return 键的详细信息或错误信息
     */
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

    /**
     * 添加一个新的键。
     *
     * @param request 包含键名、类型和值的请求信息
     * @return 成功或失败信息
     */
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

    /**
     * 更新指定键的值。
     *
     * @param key     要更新的键
     * @param request 包含类型和值的更新信息
     * @return 成功或失败信息
     */
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

    /**
     * 删除指定键。
     *
     * @param key 要删除的键
     * @return 成功或失败信息
     */
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

    /**
     * 设置指定键的过期时间。
     *
     * @param key     要设置的键
     * @param request 包含过期时间的请求信息
     * @return 成功或失败信息
     */
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

    /**
     * 获取当前连接的 Redis 信息。
     *
     * @return 包含连接信息的 Map 或错误信息
     */
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