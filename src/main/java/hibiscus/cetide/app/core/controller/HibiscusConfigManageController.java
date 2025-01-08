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
     * 备份配置文件。
     *
     * @param request 包含文件路径、操作员、描述和内容的请求信息。
     * @return 成功时返回备份的历史记录对象，失败时返回错误信息。
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
     * 获取指定配置文件的修改历史记录。
     *
     * @param filePath 配置文件路径。
     * @return 历史记录列表或错误信息。
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
     * 回滚配置文件到指定的历史版本。
     *
     * @param historyId 历史版本ID。
     * @return 成功或失败信息。
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
     * 验证配置文件内容是否有效。
     *
     * @param request 包含配置文件内容的请求。
     * @return 验证结果，成功时返回是否有效，失败时返回错误信息。
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
     * 比较两个配置文件版本的差异。
     *
     * @param historyId1 第一个版本的历史ID。
     * @param historyId2 第二个版本的历史ID。
     * @return 差异信息或错误信息。
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
     * 获取指定配置文件的内容。
     *
     * @param filePath 配置文件路径。
     * @return 配置文件内容或错误信息。
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
     * 获取指定配置文件的当前版本ID。
     *
     * @param filePath 配置文件路径。
     * @return 当前版本ID或错误信息。
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