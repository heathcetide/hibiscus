package hibiscus.cetide.app.basic.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import hibiscus.cetide.app.core.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    
    // 定义公开路径
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/api/hibiscus/auth/login",
        "/error",
        "/css/",
        "/js/",
        "/images/",
        "/lib/",
        "/fonts/"
    );

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthInterceptor(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();
        log.debug("处理请求: {}", path);

        // 检查是否是公开路径
        if (isPublicPath(path)) {
            return true;
        }

        // 首先从请求参数中获取token
        String token = request.getParameter("token");
        
        // 如果URL参数中没有token，则从请求头中获取
        if (token == null) {
            String bearerToken = request.getHeader("Authorization");
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                token = bearerToken.substring(7);
            }
        }
        log.info("token: {}", token);
        if (token != null && authService.validateToken(token)) {
            return true;
        }

        // 检查是否是AJAX请求
        String xRequestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        boolean isAjax = "XMLHttpRequest".equals(xRequestedWith) 
            || (accept != null && accept.contains("application/json"));

        if (isAjax) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, String> result = new HashMap<>();
            result.put("error", "未授权访问");
            result.put("message", "请先登录");
            objectMapper.writeValue(response.getWriter(), result);
        } else {
            response.sendRedirect("/api/hibiscus/auth/login");
        }

        return false;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(publicPath -> 
            path.equals(publicPath) || path.startsWith(publicPath));
    }
} 