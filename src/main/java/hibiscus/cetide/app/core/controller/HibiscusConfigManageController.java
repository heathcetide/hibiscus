package hibiscus.cetide.app.core.controller;

import hibiscus.cetide.app.core.model.ConfigHistory;
import hibiscus.cetide.app.core.service.HibiscusConfigManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hibiscus/config")
public class HibiscusConfigManageController {
    
    @Autowired
    private HibiscusConfigManageService configManageService;

    /**
     * 备份配置文件
     */
    @PostMapping("/backup")
    public ResponseEntity<?> backupConfig(@RequestBody Map<String, String> request) {
        try {
            String filePath = request.get("filePath");
            String operator = request.get("operator");
            String description = request.get("description");
            String content = request.get("content");
            
            ConfigHistory history = configManageService.saveConfig(filePath, content, operator, description);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new HashMap<>().put("error", e.getMessage()));
        }
    }

    /**
     * 获取配置文件的修改历史
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam String filePath) {
        try {
            List<ConfigHistory> history = configManageService.getHistory(filePath);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new HashMap<>().put("error", e.getMessage()));
        }
    }

    /**
     * 回滚配置文件到指定版本
     */
    @PostMapping("/rollback/{historyId}")
    public ResponseEntity<?> rollbackConfig(@PathVariable Long historyId) {
        try {
            configManageService.rollbackConfig(historyId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "回滚成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 验证配置文件内容
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateConfig(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            boolean isValid = configManageService.validateConfig(content);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("valid", isValid);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 比较两个配置文件版本的差异
     */
    @GetMapping("/compare")
    public ResponseEntity<?> compareConfigs(
            @RequestParam Long historyId1,
            @RequestParam Long historyId2) {
        try {
            Map<String, Object> comparison = configManageService.compareConfigs(historyId1, historyId2);
            return ResponseEntity.ok(comparison);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new HashMap<>().put("error", e.getMessage()));
        }
    }

    /**
     * 获取配置文件内容
     */
    @GetMapping("/content")
    public ResponseEntity<?> getConfigContent(@RequestParam String filePath) {
        try {
            String content = configManageService.getConfigContent(filePath);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("content", content);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取当前版本ID
     */
    @GetMapping("/current-version")
    public ResponseEntity<?> getCurrentVersion(@RequestParam String filePath) {
        try {
            Long currentVersionId = configManageService.getCurrentVersionId(filePath);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("versionId", currentVersionId);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 