package hibiscus.cetide.app.common;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class ApiUrlUtil {

    @Value("${server.host:http://localhost}")
    private String serverHost;

    @Value("${server.port:8080}")
    private String serverPort;

    public String buildApiUrl(String path) {
        return serverHost + ":" + serverPort + path;
    }

    public String getServerUrl() {
        return serverHost + ":" + serverPort;
    }
}
