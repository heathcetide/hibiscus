package hibiscus.cetide.app.basic.interceptor;

import hibiscus.cetide.app.core.service.HibiscusMonitorService;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class ApiMetricsInterceptor implements HandlerInterceptor {
    
    private final HibiscusMonitorService monitorService;
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    public ApiMetricsInterceptor(HibiscusMonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        startTime.set(System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            long duration = System.currentTimeMillis() - startTime.get();
            String path = request.getRequestURI();
            int statusCode = response.getStatus();
            
            // 记录API调用信息
            monitorService.recordApiCall(path, duration, statusCode);
        } finally {
            startTime.remove();
        }
    }
} 