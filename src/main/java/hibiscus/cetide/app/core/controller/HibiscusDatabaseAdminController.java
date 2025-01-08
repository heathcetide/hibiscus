package hibiscus.cetide.app.core.controller;

import hibiscus.cetide.app.core.service.HibiscusDatabaseAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hibiscus/database")
public class HibiscusDatabaseAdminController {
    
    private final HibiscusDatabaseAdminService databaseAdminService;
    
    public HibiscusDatabaseAdminController(HibiscusDatabaseAdminService databaseAdminService) {
        this.databaseAdminService = databaseAdminService;
    }
    
    @GetMapping("/check-connection")
    public Map<String, Object> checkConnection() {
        return databaseAdminService.checkConnection();
    }
    
    @GetMapping("/table/{tableName}")
    public ResponseEntity<?> getTableData(@PathVariable("tableName") String tableName) {
        try {
            List<Map<String, Object>> data = databaseAdminService.getTableData(tableName);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.ok()
                .body(new HashMap<>().put("message", e.getMessage()));
        }
    }
    
    @PutMapping("/table/{tableName}")
    public ResponseEntity<?> updateTableData(
            @PathVariable("tableName") String tableName,
            @RequestParam("primaryKey") String primaryKey,
            @RequestParam("primaryKeyValue") String primaryKeyValue,
            @RequestBody Map<String, Object> data) {
        try {
            databaseAdminService.updateTableData(tableName, primaryKey, primaryKeyValue, data);
            return ResponseEntity.ok(new HashMap<>().put("message", "更新成功"));
        } catch (Exception e) {
            return ResponseEntity.ok()
                .body(new HashMap<>().put("message", e.getMessage()));
        }
    }
    
    @DeleteMapping("/table/{tableName}")
    public ResponseEntity<?> deleteTableData(
            @PathVariable("tableName") String tableName,
            @RequestParam("primaryKey") String primaryKey,
            @RequestParam("primaryKeyValue") String primaryKeyValue) {
        try {
            databaseAdminService.deleteTableData(tableName, primaryKey, primaryKeyValue);
            return ResponseEntity.ok(new HashMap<>().put("message", "删除成功"));
        } catch (Exception e) {
            return ResponseEntity.ok()
                .body(new HashMap<>().put("message", e.getMessage()));
        }
    }
} 