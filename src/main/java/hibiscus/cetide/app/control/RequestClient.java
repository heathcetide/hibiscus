package hibiscus.cetide.app.control;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hibiscus.cetide.app.model.RequestParams;
import hibiscus.cetide.app.service.HttpRequestStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        Map<String, Object> map = gson.fromJson(requestParams.getParams(), new TypeToken<Map<String, Object>>(){}.getType());
        HttpRequestStrategy getStrategy = factory.createStrategy(requestParams.getMethod());
        return getStrategy.execute(requestParams.getUrl(), map);
    }
}
