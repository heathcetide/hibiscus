package hibiscus.cetide.app.module.control;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hibiscus.cetide.app.common.model.RequestInfo;
import hibiscus.cetide.app.common.model.RequestParams;
import hibiscus.cetide.app.module.service.HttpRequestStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static hibiscus.cetide.app.core.MappingHandler.requestInfos;


@RestController
@RequestMapping("/hibiscus/test")
public class RequestClient {
    @Autowired
    HttpRequestStrategyFactory factory;

    @GetMapping("/hello")
    public String test() {
        return "test";
    }

    @PostMapping("/interface")
    public String testInterface(@RequestBody RequestParams requestParams) {
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(requestParams.getParams(), new TypeToken<Map<String, Object>>() {
        }.getType());
        HttpRequestStrategy getStrategy = factory.createStrategy(requestParams.getMethod());
        return getStrategy.execute(requestParams.getUrl(), map);
    }

    @GetMapping("/request/info/{className}")
    public List<RequestInfo> getRequestInfo(@PathVariable String className) {
        // 创建一个新的列表来存储符合条件的 RequestInfo 对象
        List<RequestInfo> filteredRequestInfos = new ArrayList<>();
        // 使用增强的 for 循环遍历列表
        for (RequestInfo requestInfo : requestInfos) {
            // 检查当前对象的 className 是否匹配
            if (requestInfo.getClassName().equals(className)) {
                // 打印相关信息
                System.out.println("Class Name: " + requestInfo.getClassName());
                System.out.println("Method Name: " + requestInfo.getMethodName());
                System.out.println("Paths: " + requestInfo.getPaths());
                System.out.println("Parameters: " + requestInfo.getParameters());
                // 将参数转换为 JSON 字符串
                Gson gson = new Gson();
                String jsonString = gson.toJson(requestInfo.getParameters());
                requestInfo.setParams(jsonString);
                // 将符合条件的对象添加到新列表中
                filteredRequestInfos.add(requestInfo);
            }
        }
        // 返回符合条件的 RequestInfo 对象列表
        return filteredRequestInfos;
    }
}
