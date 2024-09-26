package hibiscus.cetide.app.core;

import hibiscus.cetide.app.basic.log.core.LogLevel;
import hibiscus.cetide.app.basic.log.core.Logger;
import hibiscus.cetide.app.basic.log.handler.LogHandler;
import hibiscus.cetide.app.basic.log.handler.impl.ConsoleLogHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

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
        LogHandler consoleHandler = new ConsoleLogHandler();
        logger.addHandler(consoleHandler);
        Class<?> mainClass = classScanner.findMainClass();
        if (mainClass != null) {
            logger.log(LogLevel.START, "Found Main Class: " + mainClass.getName());
            classScanner.scanApplication(mainClass);
        } else {
            logger.log(LogLevel.ERROR, "No Main Class Found.");
        }
    }
}

