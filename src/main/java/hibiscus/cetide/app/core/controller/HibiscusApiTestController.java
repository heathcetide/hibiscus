package hibiscus.cetide.app.core.controller;

import hibiscus.cetide.app.core.model.RequestInfo;
import hibiscus.cetide.app.core.scan.MappingHandler;
import hibiscus.cetide.app.core.service.HibiscusApiCollectorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api-test")
public class HibiscusApiTestController {
    
    private final HibiscusApiCollectorService apiCollectorService;
    private final MappingHandler mappingHandler;
    
    public HibiscusApiTestController(HibiscusApiCollectorService apiCollectorService, MappingHandler mappingHandler) {
        this.apiCollectorService = apiCollectorService;
        this.mappingHandler = mappingHandler;
    }
    
    @GetMapping("/list")
    public Map<String, HibiscusApiCollectorService.ApiInfo> listApis() {
        return apiCollectorService.getAllApiInfo();
    }
    
    @GetMapping("/scan-list")
    public List<RequestInfo> listScannedApis() {
        return mappingHandler.getRequestInfos();
    }
} 