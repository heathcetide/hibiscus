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

    /**
     * 加载接口测试页面。
     *
     * @param model 用于传递API信息到前端
     * @return 返回接口测试页面路径
     */
    @GetMapping
    public String interfaceTestPage(Model model) {
        Map<String, HibiscusApiCollectorService.ApiInfo> apiInfos = apiCollectorService.getAllApiInfo();
        model.addAttribute("apiInfos", apiInfos);
        return "interface/index";
    }

    /**
     * 测试指定的接口。
     *
     * @param request 测试请求，包括URL、方法、头信息、参数和请求体
     * @return 测试结果的响应，或者在出错时返回错误信息
     */
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

    /**
     * 获取所有API的列表。
     *
     * @return 包含所有API信息的Map
     */
    @GetMapping("/api-list")
    @ResponseBody
    public Map<String, HibiscusApiCollectorService.ApiInfo> getApiList() {
        return apiCollectorService.getAllApiInfo();
    }

    /**
     * 获取指定API的详细信息。
     *
     * @param className  API所属的类名
     * @param methodName API的方法名
     * @return 返回匹配的方法信息，或者返回null
     */
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

    /**
     * 接口请求类，封装测试接口所需的参数。
     */
    public static class TestRequest {
        private String url;
        private String method;
        private Map<String, String> headers;
        private Map<String, String> params;  // 添加查询参数
        private Object body;

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