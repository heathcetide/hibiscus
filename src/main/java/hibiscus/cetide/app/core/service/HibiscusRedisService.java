package hibiscus.cetide.app.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import hibiscus.cetide.app.config.HibiscusRedisConfig;
import hibiscus.cetide.app.config.RedisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class HibiscusRedisService {
    @Autowired
    private HibiscusRedisConfig redisConfig;

    @Autowired
    private RedisProperties redisProperties;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static Logger log = LoggerFactory.getLogger(HibiscusRedisService.class);

    private volatile boolean isConnected = false;

    @PostConstruct
    public void init() {
        // 如果配置文件中启用了Redis，尝试建立连接
        if (redisProperties.isEnabled()) {
            try {
                connect(
                    redisProperties.getHost(),
                    redisProperties.getPort(),
                    redisProperties.getPassword(),
                    redisProperties.getDatabase()
                );
            } catch (Exception e) {
                isConnected = false;
                log.error("无法使用配置文件中的Redis配置: {}", e.getMessage());
            }
        }
    }

    public Map<String, Object> getConnectionInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("connected", isConnected());
        info.put("host", redisProperties.getHost());
        info.put("port", redisProperties.getPort());
        info.put("database", redisProperties.getDatabase());
        return info;
    }

    public void connect(String host, int port, String password, int database) {
        try {
            redisConfig.updateRedisConnection(host, port, password, database);
            // 测试连接
            redisTemplate.getConnectionFactory().getConnection().ping();
            isConnected = true;
            
            // 更新配置
            redisProperties.setHost(host);
            redisProperties.setPort(port);
            redisProperties.setPassword(password);
            redisProperties.setDatabase(database);
            redisProperties.setEnabled(true);
        } catch (Exception e) {
            isConnected = false;
            throw new RuntimeException("Redis连接失败: " + e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        if (!isConnected) {
            return false;
        }
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            isConnected = false;
            return false;
        }
    }

    private void checkConnection() {
        if (!isConnected()) {
            throw new RuntimeException("Redis未连接或连接已断开");
        }
    }

    public List<Map<String, Object>> getKeys(String pattern) {
        checkConnection();
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null) return new ArrayList<>();

        return keys.stream().map(key -> {
            Map<String, Object> keyInfo = new HashMap<>();
            keyInfo.put("name", key);
            
            DataType type = redisTemplate.type(key);
            keyInfo.put("type", type.name().toLowerCase());
            
            Long ttl = redisTemplate.getExpire(key);
            keyInfo.put("ttl", ttl != null ? ttl : -1);
            
            Object value = getValue(key, type);
            keyInfo.put("value", value);
            
            return keyInfo;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getKey(String key) {
        checkConnection();
        DataType type = redisTemplate.type(key);
        if (type == DataType.NONE) {
            return null;
        }

        Map<String, Object> keyInfo = new HashMap<>();
        keyInfo.put("type", type.name().toLowerCase());
        keyInfo.put("value", getValue(key, type));
        return keyInfo;
    }

    public void setKey(String key, String type, Object value) {
        checkConnection();
        switch (type.toLowerCase()) {
            case "string":
                redisTemplate.opsForValue().set(key, value.toString());
                break;
            case "list":
                redisTemplate.delete(key);
                if (value instanceof List) {
                    redisTemplate.opsForList().rightPushAll(key, (List<?>) value);
                }
                break;
            case "set":
                redisTemplate.delete(key);
                if (value instanceof Set) {
                    redisTemplate.opsForSet().add(key, ((Set<?>) value).toArray());
                }
                break;
            case "hash":
                redisTemplate.delete(key);
                if (value instanceof Map) {
                    redisTemplate.opsForHash().putAll(key, (Map<?, ?>) value);
                }
                break;
            case "zset":
                redisTemplate.delete(key);
                if (value instanceof List) {
                    for (Map<String, Object> entry : (List<Map<String, Object>>) value) {
                        redisTemplate.opsForZSet().add(key, entry.get("member"), 
                            Double.parseDouble(entry.get("score").toString()));
                    }
                }
                break;
        }
    }

    public void deleteKey(String key) {
        checkConnection();
        redisTemplate.delete(key);
    }

    public void setExpire(String key, long seconds) {
        checkConnection();
        if (seconds < 0) {
            redisTemplate.persist(key);
        } else {
            redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
        }
    }

    private Object getValue(String key, DataType type) {
        switch (type) {
            case STRING:
                return redisTemplate.opsForValue().get(key);
            case LIST:
                Long size = redisTemplate.opsForList().size(key);
                return size != null ? redisTemplate.opsForList().range(key, 0, size - 1) : new ArrayList<>();
            case SET:
                return redisTemplate.opsForSet().members(key);
            case HASH:
                return redisTemplate.opsForHash().entries(key);
            case ZSET:
                Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
                if (tuples == null) return new ArrayList<>();
                return tuples.stream()
                    .map(tuple -> {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("member", tuple.getValue());
                        entry.put("score", tuple.getScore());
                        return entry;
                    })
                    .collect(Collectors.toList());
            default:
                return null;
        }
    }
} 