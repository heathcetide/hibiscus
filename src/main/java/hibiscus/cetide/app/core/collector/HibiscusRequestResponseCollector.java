package hibiscus.cetide.app.core.collector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 一个组件，用于收集API请求和响应的样例数据。
 */
@Component
public class HibiscusRequestResponseCollector {
    private static final Logger log = LoggerFactory.getLogger(HibiscusRequestResponseCollector.class);
    private static final int MAX_SAMPLES = 5;  // 每个接口保留的样例数量
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Queue<ApiSample>> samples = new ConcurrentHashMap<>();

    /**
     * 收集指定API路径的请求和响应样例。
     *
     * @param path      API路径
     * @param request   请求对象
     * @param response  响应对象
     * @param timestamp 请求发生的时间戳
     */
    public void collectSample(String path, Object request, Object response, long timestamp) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            String responseJson = objectMapper.writeValueAsString(response);
            
            Queue<ApiSample> pathSamples = samples.computeIfAbsent(path, 
                k -> new ConcurrentLinkedQueue<>());
            
            // 保持样例数量在限制内
            if (pathSamples.size() >= MAX_SAMPLES) {
                pathSamples.poll();
            }
            
            pathSamples.offer(new ApiSample(requestJson, responseJson, timestamp));
            log.debug("Collected sample for path: {}", path);
        } catch (Exception e) {
            log.error("Failed to collect API sample", e);
        }
    }

    /**
     * 获取指定API路径的请求和响应样例。
     *
     * @param path API路径
     * @return 样例队列，如果路径没有样例则返回空队列。
     */
    public Queue<ApiSample> getSamples(String path) {
        return samples.getOrDefault(path, new ConcurrentLinkedQueue<>());
    }

    /**
     * API请求和响应的样例数据。
     */
    public static class ApiSample {
        private final String request;
        private final String response;
        private final long timestamp;
        
        public ApiSample(String request, String response, long timestamp) {
            this.request = request;
            this.response = response;
            this.timestamp = timestamp;
        }
        
        public String getRequest() { return request; }
        public String getResponse() { return response; }
        public long getTimestamp() { return timestamp; }
    }
} 