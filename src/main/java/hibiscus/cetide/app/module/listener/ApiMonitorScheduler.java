package hibiscus.cetide.app.module.listener;

import hibiscus.cetide.app.module.service.ApiMonitorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ApiMonitorScheduler {

    private final ApiMonitorService apiMonitorService;

    public ApiMonitorScheduler(ApiMonitorService apiMonitorService) {
        this.apiMonitorService = apiMonitorService;
    }

    // 每隔 5 分钟检查一次 API 状态
    @Scheduled(fixedRate = 300000)
    public void monitorApis() {
        System.out.println("Checking API status...");
        apiMonitorService.checkApiStatus();
    }
}
