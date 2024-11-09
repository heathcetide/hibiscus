package hibiscus.cetide.app.module.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import hibiscus.cetide.app.module.service.HttpRequestStrategy;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PostRequestStrategy implements HttpRequestStrategy {

    @Override
    public String execute(String url, Map<String, String> headers, String body) {
        // 检查 Content-Type 头是否存在，并根据其值决定是否转换 body
        boolean isFormData = false;
        if (headers != null) {
            String contentType = headers.get("Content-Type");
            if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
                isFormData = true;
            }
        }

        if (isFormData && body != null && !body.isEmpty()) {
            body = convertToFormData(body);
        }

        HttpRequest request = HttpRequest.post(url);
        // 添加请求头
        if (headers != null) {
            headers.forEach(request::header);
        }

        // 添加请求体
        if (body != null && !body.isEmpty()) {
            request.body(body);
        }

        System.out.println("request: " + request);
        // 执行请求并获取响应
        HttpResponse response = request.execute();
        System.out.println("response: " + response);
        return response.body();
    }


//    @Override
//    public String execute(String url, Map<String, String> headers, String body) {
//        // 检查 Content-Type 头是否存在，并根据其值决定是否转换 body
//        boolean isFormData = false;
//        if (headers != null) {
//            String contentType = headers.get("Content-Type");
//            if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
//                isFormData = true;
//            }
//        }
//
//        if (isFormData && body != null && !body.isEmpty()) {
//            body = convertToFormData(body);
//        }
//
//        HttpRequest request = HttpRequest.post(url);
//        // 添加请求头
//        if (headers != null) {
//            headers.forEach(request::header);
//        }
//
//        // 添加请求体
//        if (body != null && !body.isEmpty()) {
//            request.body(body);
//        }
//
//        System.out.println("request: " + request);
//        // 执行请求并获取响应
//        HttpResponse response = request.execute();
//        System.out.println("response: " + response);
//        return response.body();
//    }

    public static String convertToFormData(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> jsonMap = objectMapper.readValue(jsonString, Map.class);
            return convertMapToFormData(jsonMap);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String convertMapToFormData(Map<String, String> jsonMap) {
        StringBuilder formData = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : jsonMap.entrySet()) {
                if (formData.length() > 0) {
                    formData.append("&");
                }
                formData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()));
                formData.append("=");
                formData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return formData.toString();
    }

}
