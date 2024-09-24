package hibiscus.cetide.app.core;

import hibiscus.cetide.app.config.AppConfigProperties;
import hibiscus.cetide.app.log.core.LogLevel;
import hibiscus.cetide.app.log.core.Logger;
import hibiscus.cetide.app.log.handler.LogHandler;
import hibiscus.cetide.app.log.handler.impl.ConsoleLogHandler;
import hibiscus.cetide.app.log.handler.impl.FileLogHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

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
    Logger logger = new Logger();
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.setLevel(LogLevel.INFO);
        LogHandler consoleHandler = new ConsoleLogHandler();
        logger.addHandler(consoleHandler);

        Class<?> mainClass = classScanner.findMainClass();
        if (mainClass != null) {
            logger.log(LogLevel.INFO, "Found main class: " + mainClass.getName());
            classScanner.scanApplication(mainClass);
        } else {
            logger.log(LogLevel.INFO, "No main class found.");
        }
    }
}

