package hibiscus.cetide.app.listener;


import hibiscus.cetide.app.config.AppConfigProperties;
import hibiscus.cetide.app.model.MethodMetrics;
import hibiscus.cetide.app.model.NetworkMetrics;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Aspect
@Component
public class ListenerAspect {

    private final Map<String, NetworkMetrics> networkMetricsMap = new HashMap<>();
    private final Map<String, MethodMetrics> methodMetricsMap = new HashMap<>();

    @Autowired
    private AppConfigProperties appConfigProperties;

    //    @Around("execution(* hibiscus..*.*.*(..))")

//    @Around("execution(* hibiscus.cetide.app.control.*.*(..))")
//    @Around("execution(* com..*.*.*(..))")
    @Around("execution(* com..*.*(..)) && @within(org.springframework.web.bind.annotation.RestController) || execution(* com..*.*(..)) && @within(org.springframework.stereotype.Controller)")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        long begin = System.currentTimeMillis();
        // 动态获取切入点表达式的包名
        String packageName = appConfigProperties.getHibiscus();
        String methodName = joinPoint.getSignature().toString().substring(7);

        if (!methodName.startsWith(packageName)) {
            return joinPoint.proceed();
        }

        // 运行原始方法
        Object object = joinPoint.proceed(); // 调用原始方法运行
        // 记录结束时间
        long end = System.currentTimeMillis();
        long time = end - begin;

        // 输出运行时间
        System.out.println(joinPoint.getSignature() + " 执行耗时 " + time);

        // 存储方法执行时间
        MethodMetrics metrics = methodMetricsMap.computeIfAbsent(methodName, k -> new MethodMetrics(methodName));
        metrics.incrementAccessCount(time);
        collectMethodMetrics(joinPoint, object, metrics);
        return object;
    }

    // 获取所有方法执行时间
    public List<MethodMetrics> getMethodMetrics() {
        return new ArrayList<>(methodMetricsMap.values());
    }

    //    execution(* ...*.*.*(..))
    private void collectMethodMetrics(ProceedingJoinPoint joinPoint, Object result, MethodMetrics metrics) {
        // 假设输入参数中有InputStream类型的参数
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof InputStream) {
                InputStream inputStream = (InputStream) arg;
                long inputBytes = calculateInputBytes(inputStream);
                metrics.incrementUploadBytes(inputBytes);
            }
        }

        // 假设返回结果中有OutputStream类型的参数
        if (result instanceof ByteArrayOutputStream) {
            ByteArrayOutputStream outputStream = (ByteArrayOutputStream) result;
            long outputBytes = outputStream.toByteArray().length;
            metrics.incrementDownloadBytes(outputBytes);
        }
    }

    private long calculateInputBytes(InputStream inputStream) {
        byte[] buffer = new byte[1024];
        long bytes = 0;
        try {
            int n;
            while ((n = inputStream.read(buffer)) != -1) {
                bytes += n;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }

}
