package hibiscus.cetide.app.core.controller;

import hibiscus.cetide.app.core.service.HibiscusApiCollectorService;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.Map;

@Controller
@RequestMapping("/api/hibiscus/interface")
public class HibiscusInterfaceTestController {
    
    private final HibiscusApiCollectorService apiCollectorService;
    
    public HibiscusInterfaceTestController(HibiscusApiCollectorService apiCollectorService) {
        this.apiCollectorService = apiCollectorService;
    }
    
    @GetMapping
    public String interfaceTestPage(Model model) {
        Map<String, HibiscusApiCollectorService.ApiInfo> apiInfos = apiCollectorService.getAllApiInfo();
        model.addAttribute("apiInfos", apiInfos);
        return "interface/index";
    }

    @PostMapping("/test")
    @ResponseBody
    public ResponseEntity<Object> testInterface(@RequestBody TestRequest request) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            if (request.getHeaders() != null) {
                request.getHeaders().forEach(headers::add);
            }
            
            // 处理URL
            String url = request.getUrl();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                // 如果是相对路径，添加基础URL
                url = "http://localhost:8080" + (url.startsWith("/") ? url : "/" + url);
            }
            
            // 构建URL（包含查询参数）
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            if (request.getParams() != null) {
                request.getParams().forEach(builder::queryParam);
            }
            
            HttpEntity<?> entity = new HttpEntity<>(request.getBody(), headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                builder.build().toUri(),
                HttpMethod.valueOf(request.getMethod()),
                entity,
                Object.class
            );
            
            return response;
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    @GetMapping("/api-list")
    @ResponseBody
    public Map<String, HibiscusApiCollectorService.ApiInfo> getApiList() {
        return apiCollectorService.getAllApiInfo();
    }
    
    @GetMapping("/api-details")
    @ResponseBody
    public HibiscusApiCollectorService.MethodInfo getApiDetails(
            @RequestParam(name = "className") String className,
            @RequestParam(name = "methodName") String methodName) {
        Map<String, HibiscusApiCollectorService.ApiInfo> apiInfos = apiCollectorService.getAllApiInfo();
        HibiscusApiCollectorService.ApiInfo apiInfo = apiInfos.get(className);
        if (apiInfo != null) {
            return apiInfo.getMethods().stream()
                    .filter(method -> method.getMethodName().equals(methodName))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
    
    public static class TestRequest {
        private String url;
        private String method;
        private Map<String, String> headers;
        private Map<String, String> params;  // 添加查询参数
        private Object body;
        
        // getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        
        public Map<String, String> getParams() { return params; }
        public void setParams(Map<String, String> params) { this.params = params; }
        
        public Object getBody() { return body; }
        public void setBody(Object body) { this.body = body; }
    }
} 