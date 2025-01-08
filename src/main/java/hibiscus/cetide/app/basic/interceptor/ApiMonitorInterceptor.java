package hibiscus.cetide.app.basic.interceptor;

import hibiscus.cetide.app.core.collector.HibiscusApiDependencyCollector;
import hibiscus.cetide.app.core.collector.HibiscusExceptionCollector;
import hibiscus.cetide.app.core.collector.HibiscusRequestResponseCollector;
import hibiscus.cetide.app.core.limiter.HibiscusApiRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class ApiMonitorInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ApiMonitorInterceptor.class);
    
    @Autowired
    private HibiscusRequestResponseCollector requestResponseCollector;
    
    @Autowired
    private HibiscusExceptionCollector exceptionCollector;
    
    @Autowired
    private HibiscusApiDependencyCollector dependencyCollector;
    
    @Autowired
    private HibiscusApiRateLimiter rateLimiter;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String path = request.getRequestURI();
        
        // 检查频率限制
        if (!rateLimiter.tryAcquire(path)) {
            response.setStatus(429);  // Too Many Requests
            return false;
        }
        
        // 记录API调用依赖关系
        dependencyCollector.enterApi(path);
        
        // 包装请求和响应以便收集内容
        if (!(request instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper(request);
        }
        if (!(response instanceof ContentCachingResponseWrapper)) {
            response = new ContentCachingResponseWrapper(response);
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                              Object handler, Exception ex) {
        String path = request.getRequestURI();
        
        // 收集请求和响应样例
        if (request instanceof ContentCachingRequestWrapper && 
            response instanceof ContentCachingResponseWrapper) {
            ContentCachingRequestWrapper requestWrapper = (ContentCachingRequestWrapper) request;
            ContentCachingResponseWrapper responseWrapper = (ContentCachingResponseWrapper) response;
            
            requestResponseCollector.collectSample(
                path,
                new String(requestWrapper.getContentAsByteArray()),
                new String(responseWrapper.getContentAsByteArray()),
                System.currentTimeMillis()
            );
        }
        
        // 记录异常
        if (ex != null) {
            exceptionCollector.recordException(path, ex);
        }
        
        // 结束API调用依赖记录
        dependencyCollector.exitApi(path);
    }
} 