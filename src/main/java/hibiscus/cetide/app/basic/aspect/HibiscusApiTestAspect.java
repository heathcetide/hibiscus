package hibiscus.cetide.app.basic.aspect;

import hibiscus.cetide.app.basic.annotation.ApiTest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class HibiscusApiTestAspect {
    
    private final Map<String, ApiInfo> API_REGISTRY = new ConcurrentHashMap<>();
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @PostConstruct
    public void init() {
        RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> methodMap = mapping.getHandlerMethods();
        
        methodMap.forEach((info, method) -> {
            Method targetMethod = method.getMethod();
            ApiTest apiTest = targetMethod.getAnnotation(ApiTest.class);
            if (apiTest != null) {
                String path = getPath(targetMethod);
                String httpMethod = getHttpMethod(targetMethod);
                
                API_REGISTRY.put(path, new ApiInfo(
                    apiTest.value(),
                    apiTest.description(),
                    httpMethod,
                    path,
                    targetMethod.getParameterTypes()
                ));
            }
        });
    }
    
    @Around("@annotation(hibiscus.cetide.app.basic.annotation.ApiTest)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        
        ApiTest apiTest = method.getAnnotation(ApiTest.class);
        String path = getPath(method);
        String httpMethod = getHttpMethod(method);
        
        API_REGISTRY.put(path, new ApiInfo(
            apiTest.value(),
            apiTest.description(),
            httpMethod,
            path,
            method.getParameterTypes()
        ));
        
        return point.proceed();
    }
    
    private String getPath(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        String basePath = "";
        if (declaringClass.isAnnotationPresent(RequestMapping.class)) {
            basePath = declaringClass.getAnnotation(RequestMapping.class).value()[0];
        }
        
        String methodPath = "";
        if (method.isAnnotationPresent(GetMapping.class)) {
            methodPath = method.getAnnotation(GetMapping.class).value().length > 0 
                ? method.getAnnotation(GetMapping.class).value()[0] : "";
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            methodPath = method.getAnnotation(PostMapping.class).value().length > 0 
                ? method.getAnnotation(PostMapping.class).value()[0] : "";
        }
        // 可以添加其他mapping类型
        
        return basePath + methodPath;
    }
    
    private String getHttpMethod(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            return "GET";
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            return "POST";
        }
        return "UNKNOWN";
    }
    
    public Map<String, ApiInfo> getApiRegistry() {
        return new HashMap<>(API_REGISTRY);
    }
    
    public static class ApiInfo {
        private final String name;
        private final String description;
        private final String method;
        private final String path;
        private final Class<?>[] parameterTypes;
        
        public ApiInfo(String name, String description, String method, String path, Class<?>[] parameterTypes) {
            this.name = name;
            this.description = description;
            this.method = method;
            this.path = path;
            this.parameterTypes = parameterTypes;
        }
        
        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getMethod() { return method; }
        public String getPath() { return path; }
        public Class<?>[] getParameterTypes() { return parameterTypes; }
    }
} 