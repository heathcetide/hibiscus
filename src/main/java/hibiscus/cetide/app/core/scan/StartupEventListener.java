package hibiscus.cetide.app.core.scan;

import hibiscus.cetide.app.basic.log.core.LogLevel;
import hibiscus.cetide.app.basic.log.core.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

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

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Class<?> mainClass = classScanner.findMainClass();
        if (mainClass != null) {
            logger.log(LogLevel.INFO, "Found Main Class: " + mainClass.getName());
            classScanner.scanApplication(mainClass);
        } else {
            logger.log(LogLevel.ERROR, "No Main Class Found.");
        }
    }
    @PostConstruct
    public void initListener() {
        logger.log(LogLevel.START,"Hibiscus Starting","Service has been started.");
    }

    @PreDestroy
    public void destroy() {
        logger.log(LogLevel.STOP,"Hibiscus Stopping","Service has been stopped.");
        logger.shutdown();
    }
}

