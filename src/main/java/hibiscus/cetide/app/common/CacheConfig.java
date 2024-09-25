package hibiscus.cetide.app.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public ExpiredLRUCache<String, Object> expiredLRUCache() {
        long expiredTimeMillis = 1000000; // 设置过期时间
        return new ExpiredLRUCache<>(1000,expiredTimeMillis);
    }
}
