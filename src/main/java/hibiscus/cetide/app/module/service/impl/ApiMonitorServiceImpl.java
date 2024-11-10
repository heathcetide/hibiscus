package hibiscus.cetide.app.module.service.impl;

import hibiscus.cetide.app.common.utils.ApiMonitoring;
import hibiscus.cetide.app.common.utils.ApiUrlUtil;
import hibiscus.cetide.app.common.utils.XmlUtil;
import hibiscus.cetide.app.common.model.ApiStatus;
import hibiscus.cetide.app.common.model.RequestInfo;
import hibiscus.cetide.app.module.service.ApiMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static hibiscus.cetide.app.core.scan.MappingHandler.requestInfos;

@Service
public class ApiMonitorServiceImpl implements ApiMonitorService {

    @Autowired
    private ApiUrlUtil apiUrlUtil;

    private final RestTemplate restTemplate = new RestTemplate();
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private long maxResponseTime = Long.MIN_VALUE;
    private long minResponseTime = Long.MAX_VALUE;
    private long totalResponseTime = 0;
    private int monitoringCount = 0;

    public void checkApiStatus() {
        for (RequestInfo requestInfo : requestInfos) {
            checkAllApiStatus(requestInfo);
        }
    }

    private void checkAllApiStatus(RequestInfo requestInfo) {
        String apiUrl = apiUrlUtil.buildApiUrl(requestInfo.getPaths().get(0));
        System.out.println("Checking API: " + apiUrl);
        HttpHeaders headers = new HttpHeaders();

        // 构造请求头，如果有需要
        if (requestInfo.getParameters() != null && requestInfo.getParameters().containsKey("headers")) {
            @SuppressWarnings("unchecked")
            Map<String, String> headersMap = (Map<String, String>) requestInfo.getParameters().get("headers");
            headers.setAll(headersMap);
        }

        HttpEntity<String> entity = new HttpEntity<>(headers);

        totalRequests.incrementAndGet();
        monitoringCount++;

        try {
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> responseEntity;

            // 优先尝试使用 HEAD 请求检查接口
            responseEntity = restTemplate.exchange(apiUrl, HttpMethod.HEAD, entity, String.class);

            long responseTime = System.currentTimeMillis() - startTime;

            // 更新统计数据
            totalResponseTime += responseTime;
            maxResponseTime = Math.max(maxResponseTime, responseTime);
            minResponseTime = (minResponseTime == 0) ? responseTime : Math.min(minResponseTime, responseTime);

            ApiStatus status = new ApiStatus();
            status.setUrl(apiUrl);
            status.setStatus("Normal");
            status.setResponseTime(responseTime);
            status.setMonitoringTime(System.currentTimeMillis());
            status.setTotalRequests(totalRequests.get());
            status.setFailureCount(failureCount.get());
            status.setErrorRate(calculateErrorRate());
            status.setAverageResponseTime(calculateAverageResponseTime());
            status.setMaxResponseTime(maxResponseTime);
            status.setMinResponseTime(minResponseTime);
            status.setResponseStatusCode(responseEntity.getStatusCodeValue());

            saveApiStatusToXml(status);
        } catch (Exception e) {
            failureCount.incrementAndGet();

            ApiStatus status = new ApiStatus();
            status.setUrl(apiUrl);
            status.setStatus("Error");
            status.setErrorDescription(e.getMessage());
            status.setMonitoringTime(System.currentTimeMillis());
            status.setTotalRequests(totalRequests.get());
            status.setFailureCount(failureCount.get());
            status.setErrorRate(calculateErrorRate());

            saveApiStatusToXml(status);
        }
    }


    private double calculateErrorRate() {
        return (double) failureCount.get() / totalRequests.get() * 100;
    }

    private double calculateAverageResponseTime() {
        return monitoringCount == 0 ? 0 : (double) totalResponseTime / monitoringCount;
    }

    public void saveApiStatusToXml(ApiStatus newStatus) {
        try {
            File file = new File("api-monitor.xml");
            ApiMonitoring monitoring;

            // 如果文件存在，加载已有的监控信息
            if (file.exists()) {
                try {
                    monitoring = XmlUtil.loadFromXml(file, ApiMonitoring.class);
                } catch (Exception e) {
                    // 如果文件内容不正确，初始化一个空的监控对象
                    monitoring = new ApiMonitoring();
                }
            } else {
                // 如果文件不存在，创建一个新的监控信息对象
                monitoring = new ApiMonitoring();
            }

            boolean isUpdated = false;

            // 检查列表中是否已经存在相同的 URL
            for (ApiStatus status : monitoring.getApiList()) {
                if (status.getUrl().equals(newStatus.getUrl())) {
                    // 如果找到相同的 URL，则更新其信息
                    status.setStatus(newStatus.getStatus());
                    status.setResponseTime(newStatus.getResponseTime());
                    status.setMonitoringTime(newStatus.getMonitoringTime());
                    status.setTotalRequests(newStatus.getTotalRequests());
                    status.setFailureCount(newStatus.getFailureCount());
                    status.setErrorRate(newStatus.getErrorRate());
                    status.setAverageResponseTime(newStatus.getAverageResponseTime());
                    status.setMaxResponseTime(newStatus.getMaxResponseTime());
                    status.setMinResponseTime(newStatus.getMinResponseTime());
                    status.setResponseStatusCode(newStatus.getResponseStatusCode());
                    status.setErrorDescription(newStatus.getErrorDescription());
                    isUpdated = true;
                    break;
                }
            }

            // 如果没有找到相同的 URL，则添加新的 API 状态
            if (!isUpdated) {
                monitoring.getApiList().add(newStatus);
            }

            // 保存更新后的监控信息到 XML 文件
            XmlUtil.saveToXml(file, monitoring);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取所有的 API 监控信息
     */
    public ApiMonitoring getAllApiStatus() {
        try {
            File file = new File("api-monitor.xml");
            if (file.exists()) {
                return XmlUtil.loadFromXml(file, ApiMonitoring.class);
            } else {
                return new ApiMonitoring(); // 文件不存在，返回空对象
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiMonitoring(); // 出现异常时返回空对象
        }
    }

}
