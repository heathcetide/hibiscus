package hibiscus.cetide.app.service.impl;

import cn.hutool.http.HttpUtil;
import hibiscus.cetide.app.service.HttpRequestStrategy;

import java.util.Map;

public class PostRequestStrategy implements HttpRequestStrategy {

    @Override
    public String execute(String url, Map<String, Object> params) {
        String result= HttpUtil.post(url, params);
        return result;
    }
}
