package hibiscus.cetide.app.common.model;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

public class RequestDetail {

    private HttpResponse response;
    private HttpRequest request;

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    @Override
    public String toString() {
        return "RequestDetail{" +
                "response=" + response +
                ", request=" + request +
                '}';
    }

    public RequestDetail(HttpResponse response, HttpRequest request) {
        this.response = response;
        this.request = request;
    }

    public RequestDetail(){

    }
}
