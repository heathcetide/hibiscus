package hibiscus.cetide.app.module.service;

import java.util.Map;

public interface HttpRequestStrategy {
    String execute(String url, Map<String, Object> params);
}
