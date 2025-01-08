package hibiscus.cetide.app.core.controller;

import hibiscus.cetide.app.core.model.DatabaseConfig;
import hibiscus.cetide.app.core.service.HibiscusCodeGenerateService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/hibiscus/code")
public class HibiscusCodeGenerateController {
    
    private final HibiscusCodeGenerateService codeGenerateService;
    
    public HibiscusCodeGenerateController(HibiscusCodeGenerateService codeGenerateService) {
        this.codeGenerateService = codeGenerateService;
    }

    /**
     * 获取代码生成的前端页面。
     *
     * @return 返回后台页面的路径。
     */
    @GetMapping("/backstage")
    public String generatePage() {
        return "backstage/index";
    }

    /**
     * 连接数据库并返回数据库中的表信息。
     *
     * @param config 数据库配置信息
     * @return 返回表信息列表，或在失败时返回错误信息。
     */
    @PostMapping("/connect-database")
    @ResponseBody
    public ResponseEntity<?> connectDatabase(@RequestBody DatabaseConfig config) {
        try {
            return ResponseEntity.ok(codeGenerateService.connectAndGetTables(config));
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根据请求生成代码。
     *
     * @param request 包含生成代码所需的配置信息
     * @return 返回生成代码的结果，或在失败时返回错误信息。
     */
    @PostMapping("/generate")
    @ResponseBody
    public ResponseEntity<?> generateCode(@RequestBody GenerateRequest request) {
        try {
            return ResponseEntity.ok(codeGenerateService.generateCode(request));
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 下载生成的代码文件（ZIP格式）。
     *
     * @return 返回生成的ZIP文件，或在失败时返回错误响应。
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadCode() {
        try {
            byte[] zipFile = codeGenerateService.generateZipFile();
            return ResponseEntity.ok()
                .header("Content-Type", "application/zip")
                .header("Content-Disposition", "attachment; filename=generated-code.zip")
                .body(zipFile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 将生成的代码文件应用到项目中。
     *
     * @param files 包含文件名和文件内容的键值对
     * @return 返回操作结果，或在失败时返回错误信息。
     */
    @PostMapping("/apply-to-project")
    @ResponseBody
    public ResponseEntity<?> applyToProject(@RequestBody Map<String, String> files) {
        try {
            codeGenerateService.applyToProject(files);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "代码已成功应用到项目中");
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 封装生成代码的请求信息。
     */
    public static class GenerateRequest {
        private String packageName;
        private String author;
        private List<String> tables;
        private List<String> options;

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public List<String> getTables() {
            return tables;
        }

        public void setTables(List<String> tables) {
            this.tables = tables;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }
    }
} 