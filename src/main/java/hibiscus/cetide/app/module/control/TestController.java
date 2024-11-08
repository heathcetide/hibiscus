package hibiscus.cetide.app.module.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/hello")
    public String test() {
        return "test";
    }

    private static final Logger logger = LoggerFactory.getLogger(RequestClient.class);

    @PostMapping("/hello1")
    public ResponseEntity<String> hello1(@RequestBody UserRequest userRequest) {
        // 记录请求体
        logger.info("Received request body: {}", userRequest);
        // 处理请求体
        String username = userRequest.getUsername();
        String response = "Hello, " + username + "!";

        // 返回响应
        return ResponseEntity.ok(response);
    }

}
