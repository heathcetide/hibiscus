package hibiscus.cetide.app.common.model;

import java.util.List;
import java.util.Map;

public class FullResponse {
    // 请求信息
    private String method;
    private String url;
    private Map<String, List<String>> requestHeaders;

    // 响应信息
    private int responseStatus;
    private Map<String, List<String>> responseHeaders;
    private String responseBody;

    // 构造方法
    public FullResponse() {}


    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, List<String>> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, List<String>> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
}
