package hibiscus.cetide.app.basic.aspect;

import hibiscus.cetide.app.annotation.SignalEmitter;

import hibiscus.cetide.app.basic.annotation.SignalHandler;
import hibiscus.cetide.app.component.signal.SignalContext;
import hibiscus.cetide.app.component.signal.SignalManager;
import hibiscus.cetide.app.component.signal.SignalPriority;
import hibiscus.cetide.app.core.collector.HibiscusSignalContextCollector;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

@Aspect
@Order(Ordered.LOWEST_PRECEDENCE) // 确保在其他Bean初始化后执行
public class SignalAspect implements ApplicationContextAware {

    private final SignalManager signalManager;
    private ApplicationContext applicationContext;
    private volatile boolean initialized = false;

    private static Logger log = LoggerFactory.getLogger(SignalAspect.class);

    public SignalAspect(SignalManager signalManager) {
        this.signalManager = signalManager;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Around("@annotation(signalEmitter)")
    public Object handleSignalEmitter(ProceedingJoinPoint joinPoint, SignalEmitter signalEmitter) throws Throwable {
        Object result = joinPoint.proceed();

        String signalName = signalEmitter.value();
        if (signalName.isEmpty()) {
            signalName = joinPoint.getSignature().getName();
        }
        // 获取收集的上下文数据
        Map<String, Object> collectedData = HibiscusSignalContextCollector.getAndClear();
        SignalContext context = new SignalContext(
                signalName,                          // 信号名称
                joinPoint.getTarget(),               // 信号源
                SignalPriority.HIGH,               // 信号优先级
                "Signal from " + signalName,         // 信号描述
                // 信号参数
                result);
        context.setIntermediateValues(collectedData);
        signalManager.emit(signalName, joinPoint.getTarget(), result, context);
        return result;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initSignalHandlers() {
        if (initialized || applicationContext == null) {
            return;
        }

        try {
            // 确保 SignalManager 已经初始化
            if (!signalManager.isRunning()) {
                log.info("Initializing SignalManager...");
                signalManager.initialize();
            }

            Map<String, Object> beans = applicationContext.getBeansWithAnnotation(org.springframework.stereotype.Component.class);

            for (Object bean : beans.values()) {
                for (Method method : bean.getClass().getMethods()) {
                    SignalHandler annotation = method.getAnnotation(SignalHandler.class);
                    if (annotation != null) {
                        registerSignalHandler(bean, method, annotation);
                    }
                }
            }

            initialized = true;
            log.info("Signal handlers initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize signal handlers", e);
            throw e;
        }
    }

    private void registerSignalHandler(Object bean, Method method, SignalHandler annotation) {
        try {
            signalManager.connect(annotation.value(), (sender, params) -> {
                try {
                    // 获取方法参数信息
                    Parameter[] parameters = method.getParameters();

                    if (parameters.length == 0) {
                        // 无参方法
                        method.invoke(bean);
                    } else {
                        // 准备方法调用的参数
                        Object[] methodParams = new Object[parameters.length];

                        // 处理每个参数
                        for (int i = 0; i < parameters.length; i++) {
                            if (i < params.length) {
                                methodParams[i] = convertParameter(params[i], parameters[i].getType());
                            } else {
                                methodParams[i] = null;
                            }
                        }

                        method.invoke(bean, methodParams);
                    }
                } catch (Exception e) {
                    log.error("Error handling signal: {} - {}",
                            annotation.value(), e.getMessage(), e);
                }
            });

            log.debug("Registered signal handler: {} for signal: {}",
                    method.getName(), annotation.value());

        } catch (Exception e) {
            log.error("Failed to register signal handler: {} for signal: {}",
                    method.getName(), annotation.value(), e);
            throw new RuntimeException("Failed to register signal handler", e);
        }
    }
    @SuppressWarnings("unchecked")
    private Object convertParameter(Object param, Class<?> targetType) {
        if (param == null) {
            return null;
        }

        try {
            // 如果参数类型已经匹配
            if (targetType.isAssignableFrom(param.getClass())) {
                return param;
            }

            // 处理基本类型转换
            if (targetType.isPrimitive()) {
                return convertPrimitive(param, targetType);
            }

            // 处理字符串转换
            if (targetType == String.class) {
                return param.toString();
            }

            // 处理枚举类型
            if (targetType.isEnum()) {
                return Enum.valueOf((Class<? extends Enum>) targetType, param.toString());
            }

            // 如果需要，这里可以添加更多的类型转换逻辑

            log.warn("Unable to convert parameter of type {} to {}",
                    param.getClass().getName(), targetType.getName());
            return null;

        } catch (Exception e) {
            log.error("Error converting parameter: {} to type {}", param, targetType, e);
            return null;
        }
    }

    private Object convertPrimitive(Object param, Class<?> targetType) {
        String value = param.toString();

        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        }
        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return Byte.parseByte(value);
        }
        if (targetType == short.class || targetType == Short.class) {
            return Short.parseShort(value);
        }
        if (targetType == char.class || targetType == Character.class) {
            return value.charAt(0);
        }

        return null;
    }

}