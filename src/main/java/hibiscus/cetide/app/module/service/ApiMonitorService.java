package hibiscus.cetide.app.module.service;

import hibiscus.cetide.app.common.ApiMonitoring;
import hibiscus.cetide.app.common.model.ApiStatus;

public interface ApiMonitorService {

    void checkApiStatus();

    void saveApiStatusToXml(ApiStatus status);

    ApiMonitoring getAllApiStatus();
}