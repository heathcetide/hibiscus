package hibiscus.cetide.app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.Resource;

@Configuration
public class HibiscusRedisConfig {

    @Resource
    private RedisProperties redisProperties;

    private volatile LettuceConnectionFactory redisConnectionFactory;

    @Bean("hibiscusRedisFactory")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            config.setPassword(redisProperties.getPassword());
        }
        config.setDatabase(redisProperties.getDatabase());
        
        redisConnectionFactory = new LettuceConnectionFactory(config);
        redisConnectionFactory.afterPropertiesSet();
        return redisConnectionFactory;
    }

    @Bean("hibiscusRedis")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    public void updateRedisConnection(String host, int port, String password, int database) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        if (password != null && !password.isEmpty()) {
            config.setPassword(password);
        }
        config.setDatabase(database);
        
        // 如果存在旧的连接工厂，先销毁它
        if (redisConnectionFactory != null) {
            redisConnectionFactory.destroy();
        }
        
        redisConnectionFactory = new LettuceConnectionFactory(config);
        redisConnectionFactory.afterPropertiesSet();
        
        // 更新RedisTemplate的连接工厂
        redisTemplate(redisConnectionFactory).setConnectionFactory(redisConnectionFactory);
    }
} 