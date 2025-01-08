package hibiscus.cetide.app.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(prefix = "helper.api-test", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ApiTestConfiguration implements WebMvcConfigurer {
    // API测试相关配置
} 