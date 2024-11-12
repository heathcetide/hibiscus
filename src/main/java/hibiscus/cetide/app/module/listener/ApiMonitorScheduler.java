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

    @Scheduled(fixedRate = 180000)
    public void monitorApis() {
        apiMonitorService.checkApiStatus();
    }
}
