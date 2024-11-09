package hibiscus.cetide.app.module.control;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hibiscus.cetide.app.common.ApiMonitoring;
import hibiscus.cetide.app.common.model.FullRequestParams;
import hibiscus.cetide.app.common.model.RequestInfo;
import hibiscus.cetide.app.module.service.ApiMonitorService;
import hibiscus.cetide.app.module.service.HttpRequestStrategy;
import hibiscus.cetide.app.module.service.impl.ApiMonitorServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static hibiscus.cetide.app.core.scan.MappingHandler.requestInfos;


@RestController
@RequestMapping("/hibiscus/test")
public class RequestClient {
    @Autowired
    HttpRequestStrategyFactory factory;


    @Autowired
    private ApiMonitorService apiMonitorService;

    /**
     * 获取所有的 API 监控信息
     */
    @GetMapping("/monitor/all")
    public ApiMonitoring getAllApiMonitoringData() {
        return apiMonitorService.getAllApiStatus();
    }

    /**
     * 获取指定类的请求信息
     */
    @GetMapping("/request/info/{className}")
    public List<RequestInfo> getRequestInfo(@PathVariable String className) {
        List<RequestInfo> filteredRequestInfos = new ArrayList<>();
        for (RequestInfo requestInfo : requestInfos) {
            if (requestInfo.getClassName().equals(className)) {
                Gson gson = new Gson();
                String jsonString = gson.toJson(requestInfo.getParameters());
                requestInfo.setParams(jsonString);
                filteredRequestInfos.add(requestInfo);
            }
        }
        return filteredRequestInfos;
    }

    /**
     * 发送请求，根据方法和路径发送请求
     */
    @PostMapping("/send-request")
    public String sendRequest(@RequestBody FullRequestParams requestParams) {
        System.out.println("Received request params: " + requestParams);
        // 获取请求的各个部分
        String method = requestParams.getMethod();
        String url = requestParams.getUrl();
        Map<String, String> queryParams = requestParams.getQueryParams();
        Map<String, String> headers = requestParams.getHeaders();
        String body = requestParams.getBody();
        String authToken = requestParams.getAuthToken();
        // 创建请求策略
        HttpRequestStrategy strategy = factory.createStrategy(method);
        // 设置请求参数
        if (authToken != null && !authToken.isEmpty()) {
            headers.put("Authorization", "Bearer " + authToken);
        }
        // 将 Query Params 拼接到 URL 中
        if (queryParams != null && !queryParams.isEmpty()) {
            StringBuilder urlWithParams = new StringBuilder(url);
            urlWithParams.append("?");
            queryParams.forEach((key, value) -> urlWithParams.append(key).append("=").append(value).append("&"));
            url = urlWithParams.toString().replaceAll("&$", "");  // 去掉最后一个多余的 "&"
        }

        // 执行请求
        return strategy.execute(url, headers, body);
    }


    /**
     * 获取接口列表，去重并格式化 className 只返回最后一个单词
     */
    @GetMapping("/interface-list")
    public List<Map<String, Object>> getInterfaceList() {
        // 使用 LinkedHashMap 保证顺序并去重
        Map<String, List<RequestInfo>> uniqueClasses = requestInfos.stream()
                .collect(Collectors.groupingBy(
                        info -> {
                            // 提取 className 的最后一个单词
                            String[] parts = info.getClassName().split("\\.");
                            return parts[parts.length - 1];
                        },
                        LinkedHashMap::new, Collectors.toList()
                ));

        // 构建符合前端需求的返回结构
        List<Map<String, Object>> apiList = new ArrayList<>();
        for (Map.Entry<String, List<RequestInfo>> entry : uniqueClasses.entrySet()) {
            Map<String, Object> classMap = new HashMap<>();
            classMap.put("className", entry.getKey()); // 不带重复的 className
            classMap.put("methods", entry.getValue()); // 所有方法列表
            apiList.add(classMap);
        }
        return apiList;
    }

}
