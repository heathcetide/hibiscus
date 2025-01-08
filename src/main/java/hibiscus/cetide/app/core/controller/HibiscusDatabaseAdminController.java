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

    /**
     * 检查数据库连接状态。
     *
     * @return 返回包含连接状态信息的Map。
     */
    @GetMapping("/check-connection")
    public Map<String, Object> checkConnection() {
        return databaseAdminService.checkConnection();
    }

    /**
     * 获取指定表的数据。
     *
     * @param tableName 表名
     * @return 返回表中的所有数据，或在失败时返回错误信息。
     */
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

    /**
     * 更新指定表的数据。
     *
     * @param tableName      表名
     * @param primaryKey     主键字段名称
     * @param primaryKeyValue 主键值
     * @param data           更新的数据内容
     * @return 返回更新成功的信息，或在失败时返回错误信息。
     */
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

    /**
     * 删除指定表中的数据。
     *
     * @param tableName      表名
     * @param primaryKey     主键字段名称
     * @param primaryKeyValue 主键值
     * @return 返回删除成功的信息，或在失败时返回错误信息。
     */
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