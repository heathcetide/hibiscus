package hibiscus.cetide.app.service.impl;

import cn.hutool.http.HttpUtil;
import hibiscus.cetide.app.service.HttpRequestStrategy;
import java.util.Map;

public class GetRequestStrategy implements HttpRequestStrategy {
    @Override
    public String execute(String url, Map<String, Object> params) {
        String result3= HttpUtil.get(url, params);
        return result3;
    }
}
