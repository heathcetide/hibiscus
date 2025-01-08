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

    /**
     * 获取所有收集的 API 信息。
     *
     * @return 包含 API 信息的 Map，键为 API 路径，值为 API 的详细信息。
     */
    @GetMapping("/list")
    public Map<String, HibiscusApiCollectorService.ApiInfo> listApis() {
        return apiCollectorService.getAllApiInfo();
    }

    /**
     * 获取所有扫描到的 API 请求信息。
     *
     * @return 包含扫描到的请求信息的列表。
     */
    @GetMapping("/scan-list")
    public List<RequestInfo> listScannedApis() {
        return mappingHandler.getRequestInfos();
    }
} 