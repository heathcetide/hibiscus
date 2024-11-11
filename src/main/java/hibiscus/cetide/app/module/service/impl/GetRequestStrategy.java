package hibiscus.cetide.app.module.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import hibiscus.cetide.app.module.service.HttpRequestStrategy;

import java.nio.charset.Charset;
import java.util.Map;

public class GetRequestStrategy implements HttpRequestStrategy {

    @Override
    public String execute(String url, Map<String, String> headers, String body) {
        // 使用 Hutool 构建 GET 请求并添加请求头
        HttpRequest request = HttpRequest.get(url);

        // 添加请求头
        if (headers != null) {
            headers.forEach(request::header);
        }

        // 执行请求并获取响应
        HttpResponse response = request.execute();

        return response.body();
    }
}
