//package hibiscus.cetide.app.common.model;
//
//public class RequestParams {
//    private String url;
//
//    private String params;
//
//    private String method;
//
//    public String getUrl() {
//        return url;
//    }
//
//    public void setUrl(String url) {
//        this.url = url;
//    }
//
//    public String getParams() {
//        return params;
//    }
//
//    public void setParams(String params) {
//        this.params = params;
//    }
//
//    public String getMethod() {
//        return method;
//    }
//
//    public void setMethod(String method) {
//        this.method = method;
//    }
//
//    @Override
//    public String toString() {
//        return "RequestParams{" +
//                "url='" + url + '\'' +
//                ", params='" + params + '\'' +
//                ", method='" + method + '\'' +
//                '}';
//    }
//}
package hibiscus.cetide.app.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Map;

public class FullRequestParams {

    private String method;  // 请求方法：GET、POST 等
    private String url;  // 请求的 URL
    private Map<String, String> queryParams;  // 查询参数
    private Map<String, String> headers;  // 请求头
    private String body;  // 请求体（可以是 JSON、raw 数据等）
    private String authToken;  // Authorization Token

    // Getters 和 Setters
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

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public String toString() {
        return "FullRequestParams{" +
                "method='" + method + '\'' +
                ", url='" + url + '\'' +
                ", queryParams=" + queryParams +
                ", headers=" + headers +
                ", body='" + body + '\'' +
                ", authToken='" + authToken + '\'' +
                '}';
    }
}
