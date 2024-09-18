package hibiscus.cetide.app.control;


import hibiscus.cetide.app.listener.ListenerAspect;
import hibiscus.cetide.app.core.model.MethodMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class MyController {

    private final Counter requestCounter;
    private final Timer responseTimeTimer;
    private final MeterRegistry registry; // 添加 MeterRegistry 作为类的成员变量

    @Autowired
    public MyController(MeterRegistry registry) {
        this.registry = registry; // 初始化 MeterRegistry

        // 创建请求计数器
        requestCounter = registry.counter("request_counter");

        // 创建响应时间计时器
        responseTimeTimer = registry.timer("response_time");
    }

    @GetMapping("/metrics")
    public Map<String, Double> getMetrics() {
        double count = responseTimeTimer.count();
        double total = responseTimeTimer.totalTime(TimeUnit.SECONDS);

        return Map.of(
                "response_time_seconds_count", count,
                "response_time_seconds_sum", total
        );
    }

    @GetMapping("/hello")
    public String hello() {
        // 增加请求计数
        requestCounter.increment();

        // 开始计时
        Timer.Sample sample = Timer.start(registry); // 使用 MeterRegistry 的 Timer.start()

        try {
            Thread.sleep(100); // 模拟耗时操作
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // 结束计时
        sample.stop(responseTimeTimer);

        return "Hello, World!";
    }


    @Autowired
    private ListenerAspect listenerAspect;

    @GetMapping("/execution-times")
    public List<MethodMetrics> getExecutionTimes() {
        return listenerAspect.getMethodMetrics();
    }
}
