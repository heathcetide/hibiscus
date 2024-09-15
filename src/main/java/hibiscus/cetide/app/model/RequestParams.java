package hibiscus.cetide.app.model;

public class RequestParams {
    private String url;

    private String params;

    private String method;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "RequestParams{" +
                "url='" + url + '\'' +
                ", params='" + params + '\'' +
                ", method='" + method + '\'' +
                '}';
    }
}
