package hibiscus.cetide.app.module.service;

import java.util.Map;

public interface HttpRequestStrategy {

    String execute(String url, Map<String, String> headers, String body);
}