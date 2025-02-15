package hibiscus.cetide.app.config;

import hibiscus.cetide.app.core.model.AppConfigProperties;
import hibiscus.cetide.app.core.scan.ClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Configuration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(AppConfigProperties.class)
public class HelperAutoConfiguration {

    @Autowired
    private ClassScanner classScanner;

    private static Logger logger = LoggerFactory.getLogger(HelperAutoConfiguration.class);
    
    @PostConstruct
    public void init() {
        Class<?> mainClass = classScanner.findMainClass();
        if (mainClass != null) {
            classScanner.scanApplication(mainClass);
        }
    }

    @PostConstruct
    public void initListener() {
        logger.info(" ---------- Hibiscus Service is Starting ----------");
    }

    @PreDestroy
    public void destroy() {
        logger.info(" ---------- Hibiscus Service has been stopped ----------");
    }

} 