package hibiscus.cetide.app.config;

import hibiscus.cetide.app.basic.interceptor.AuthInterceptor;
import hibiscus.cetide.app.basic.interceptor.ApiMetricsInterceptor;
import hibiscus.cetide.app.basic.interceptor.PerformanceInterceptor;
import hibiscus.cetide.app.core.service.HibiscusMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.concurrent.TimeUnit;

@Configuration
public class HibiscusWebMvcConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final PerformanceInterceptor performanceInterceptor;
    private final HibiscusMonitorService monitorService;

    @Autowired
    public HibiscusWebMvcConfig(AuthInterceptor authInterceptor, PerformanceInterceptor performanceInterceptor, HibiscusMonitorService monitorService) {
        this.authInterceptor = authInterceptor;
        this.performanceInterceptor = performanceInterceptor;
        this.monitorService = monitorService;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(performanceInterceptor)
               .excludePathPatterns("/favicon.ico");
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/hibiscus/**/**")
                .excludePathPatterns(
                        "/api/hibiscus/interface/**",
                        "/api/hibiscus/auth/**",
                        "/login",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/lib/**",
                        "/fonts/**"
                );
        // 添加API监控拦截器，并设置拦截路径
        registry.addInterceptor(new ApiMetricsInterceptor(monitorService))
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(     // 排除一些不需要监控的路径
                        "/error",
                        "/favicon.ico",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                );
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/favicon.ico")
               .addResourceLocations("classpath:/static/")
               .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS));
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 添加视图控制器，将/login路径映射到login视图
        registry.addViewController("/api/hibiscus/auth/login").setViewName("login");
        registry.addViewController("/api/hibiscus/code/backstage").setViewName("backstage/index");
    }
} 