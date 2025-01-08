package hibiscus.cetide.app.basic.interceptor;

import hibiscus.cetide.app.core.service.HibiscusPerformanceAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component("performanceInterceptor")
public class PerformanceInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(PerformanceInterceptor.class);
    private final HibiscusPerformanceAnalyzer performanceAnalyzer;
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();
    
    public PerformanceInterceptor(HibiscusPerformanceAnalyzer performanceAnalyzer) {
        this.performanceAnalyzer = performanceAnalyzer;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        startTime.set(System.currentTimeMillis());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                              Object handler, Exception ex) {
        long duration = System.currentTimeMillis() - startTime.get();
        String path = request.getRequestURI();
        int status = response.getStatus();
        
        performanceAnalyzer.recordApiCall(path, duration, status);
        log.debug("Request completed: path={}, duration={}ms, status={}", 
            path, duration, status);
        
        startTime.remove();
    }
} 