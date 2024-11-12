package hibiscus.cetide.app.core.scan;

import hibiscus.cetide.app.basic.log.core.LogLevel;
import hibiscus.cetide.app.basic.log.core.Logger;
import hibiscus.cetide.app.common.utils.AppConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;

/**
 * 监听启动事件
 */
@Configuration
@ComponentScan
@Component("startupEventListener")
public class StartupEventListener implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private ClassScanner classScanner;

    @Autowired
    private Logger logger;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();

        // 获取主类
        Class<?> scanMainClass = getMainClass(context);

        if (scanMainClass.isAnnotationPresent(SpringBootApplication.class)) {
            SpringBootApplication annotation = scanMainClass.getAnnotation(SpringBootApplication.class);

            // 获取注解中的属性值
            String[] scanBasePackages = annotation.scanBasePackages();

            // 打印或处理这些属性值
            for (String basePackage : scanBasePackages) {
                if (basePackage != null && !basePackage.isEmpty() && !basePackage.equals("hibiscus.cetide.app")){
                    appConfigProperties.setHibiscus(basePackage);
                }
            }
        }
        logger.log(LogLevel.START, "appConfigProperties name: " + appConfigProperties.getHibiscus());
        Class<?> mainClass = classScanner.findMainClass();
        if (mainClass != null) {
            logger.log(LogLevel.INFO, "Found Main Class: " + mainClass.getName());
            classScanner.scanApplication(mainClass);
        } else {
            logger.log(LogLevel.ERROR, "No Main Class Found.");
        }
    }

    private Class<?> getMainClass(ApplicationContext context) {
        // 获取所有bean的名称
        String[] beanNames = context.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            try {
                Class<?> beanClass = context.getType(beanName);
                if (beanClass.isAnnotationPresent(SpringBootApplication.class)) {
                    return beanClass;
                }
            } catch (Exception e) {
                // 忽略无法获取类型的bean
            }
        }
        throw new IllegalStateException("无法找到主类");
    }
    @PostConstruct
    public void initListener() {
        logger.log(LogLevel.START,"Hibiscus Starting","Service has been started.");

    }

    @PreDestroy
    public void destroy() {
        File file = new File("api-monitor.xml");
        // 检查文件是否存在
        if (file.exists()) {
            // 尝试删除文件
            if (file.delete()) {
                logger.log(LogLevel.CRITICAL, "DELETE API-Monitor.xml Successful");
            } else {
                logger.log(LogLevel.CRITICAL, "Failed to delete API-Monitor.xml");
            }
        } else {
            logger.log(LogLevel.WARNING, "API-Monitor.xml does not exist");
        }
        logger.log(LogLevel.STOP,"Hibiscus Stopping","Service has been stopped.");
        logger.shutdown();
    }
}

