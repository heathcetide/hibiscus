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
    
    @GetMapping("/backstage")
    public String generatePage() {
        return "backstage/index";
    }
    
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