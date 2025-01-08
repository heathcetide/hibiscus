//package hibiscus.cetide.app.component.cache.examples;
//
//import hibiscus.cetide.app.component.cache.HibiscusCache;
//import hibiscus.cetide.app.component.cache.exception.CacheException;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Supplier;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.CompletionException;
//
///**
// * HibiscusCache使用示例
// *
// * 1. 首先在项目的pom.xml中添加依赖：
// * <dependency>
// *     <groupId>hibiscus.cetide</groupId>
// *     <artifactId>hibiscus-cache</artifactId>
// *     <version>${version}</version>
// * </dependency>
// *
// * 2. 在application.yml中配置缓存：
// * hibiscus:
// *   cache:
// *     enabled: true
// *     monitor:
// *       enabled: true
// *       interval: 5
// *       time-unit: SECONDS
// *     instances:
// *       userCache:
// *         max-size: 1000
// *         metrics-enabled: true
// *         compression-enabled: false
// *         time-to-live: 3600
// *         time-unit: SECONDS
// *         eviction-policy: LRU
// *       productCache:
// *         max-size: 2000
// *         compression-enabled: true
// *         time-to-live: 1800
// *         eviction-policy: LFU
// *         # 获取缓存健康状态
// * curl http://localhost:8080/api/monitor/cache/health
// *
// * # 获取缓存统计信息
// * curl http://localhost:8080/api/monitor/cache/stats
// * 简单的配置方式
// * 多种使用方式
// * 异常处理
// * 类型安全
// * 异步操作支持
// * 监控功能
// * 批量操作支持
// */
//@Service
//public class HibiscusCacheUsageExample {
//
//    // 方式1：直接注入缓存Map
//    @Autowired
//    private Map<String, HibiscusCache<Object, Object>> caches;
//
//    // 方式2：注入缓存管理器
//    @Autowired
//    private HibiscusCacheManager cacheManager;
//
//    /**
//     * 使用方式1：直接使用缓存Map
//     */
//    public User getUserById(String userId) throws CacheException {
//        HibiscusCache<Object, Object> userCache = caches.get("userCache");
//
//        // 尝试从缓存获取
//        User user = (User) userCache.get(userId);
//        if (user == null) {
//            // 缓存未命中，从数据库查询
//            user = getUserFromDatabase(userId);
//            // 放入缓存
//            try {
//                userCache.put(userId, user);
//            } catch (Exception e) {
//                // 处理异常
//            }
//        }
//        return user;
//    }
//
//    /**
//     * 使用方式2：通过缓存管理器
//     */
//    public Product getProductById(String productId) {
//        return cacheManager.get("productCache", productId, Product.class, () -> {
//            // 缓存未命中时的加载逻辑
//            return getProductFromDatabase(productId);
//        });
//    }
//
//    /**
//     * 使用方式3：批量操作
//     */
//    public void batchUpdateProducts(Map<String, Product> products) {
//        HibiscusCache<Object, Object> productCache = caches.get("productCache");
//        try {
//            // 将输入Map转换为所需类型
//            Map<Object, Object> convertedMap = convertMap(products);
//            productCache.putAll(convertedMap);
//        } catch (Exception e) {
//            // 处理异常
//        }
//    }
//
//    /**
//     * 工具方法：安全地转换Map类型
//     */
//    private <K, V> Map<Object, Object> convertMap(Map<K, V> input) {
//        Map<Object, Object> result = new HashMap<>();
//        input.forEach((key, value) -> result.put(key, value));
//        return result;
//    }
//
//    /**
//     * 使用方式4：异步写入
//     */
//    public void updateUserAsync(String userId, User user) {
//        cacheManager.putAsync("userCache", userId, user)
//            .thenRun(() -> {
//                // 写入完成后的回调
//                System.out.println("User cache updated: " + userId);
//            })
//            .exceptionally(throwable -> {
//                // 处理异常
//                System.err.println("Failed to update user cache: " + throwable.getMessage());
//                return null;
//            });
//    }
//
//    // 模拟实体类
//    public static class User {
//        private String id;
//        private String name;
//        // getters and setters
//    }
//
//    public static class Product {
//        private String id;
//        private String name;
//        // getters and setters
//    }
//
//    // 模拟数据库操作
//    private User getUserFromDatabase(String userId) {
//        // 实际项目中这里是数据库查询
//        return new User();
//    }
//
//    private Product getProductFromDatabase(String productId) {
//        // 实际项目中这里是数据库查询
//        return new Product();
//    }
//}
//
///**
// * 缓存管理器：简化缓存操作，支持泛型
// */
////@Component
//class HibiscusCacheManager {
//    private final Map<String, HibiscusCache<Object, Object>> caches;
//
//    public HibiscusCacheManager(Map<String, HibiscusCache<Object, Object>> caches) {
//        this.caches = caches;
//    }
//
//    public <K, V> void putAll(String cacheName, Map<K, V> entries) {
//        HibiscusCache<Object, Object> cache = getCache(cacheName);
//        Map<Object, Object> convertedMap = new HashMap<>();
//        entries.forEach((key, value) -> convertedMap.put(key, value));
//        cache.putAll(convertedMap);
//    }
//
//    private HibiscusCache<Object, Object> getCache(String cacheName) {
//        HibiscusCache<Object, Object> cache = caches.get(cacheName);
//        if (cache == null) {
//            throw new IllegalArgumentException("Cache not found: " + cacheName);
//        }
//        return cache;
//    }
//
//    public <T> T get(String cacheName, Object key, Class<T> type) {
//        HibiscusCache<Object, Object> cache = caches.get(cacheName);
//        if (cache == null) {
//            throw new IllegalArgumentException("Cache not found: " + cacheName);
//        }
//        try {
//            return type.cast(cache.get(key));
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to get value from cache", e);
//        }
//    }
//
//    public <T> T get(String cacheName, Object key, Class<T> type, Supplier<T> loader) {
//        T value = get(cacheName, key, type);
//        if (value == null && loader != null) {
//            value = loader.get();
//            if (value != null) {
//                put(cacheName, key, value);
//            }
//        }
//        return value;
//    }
//
//    public void put(String cacheName, Object key, Object value) {
//        HibiscusCache<Object, Object> cache = caches.get(cacheName);
//        if (cache == null) {
//            throw new IllegalArgumentException("Cache not found: " + cacheName);
//        }
//        try {
//            cache.put(key, value);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to put value to cache", e);
//        }
//    }
//
//    public CompletableFuture<Void> putAsync(String cacheName, Object key, Object value) {
//        HibiscusCache<Object, Object> cache = caches.get(cacheName);
//        if (cache == null) {
//            throw new IllegalArgumentException("Cache not found: " + cacheName);
//        }
//        return CompletableFuture.runAsync(() -> {
//            try {
//                cache.put(key, value);
//            } catch (Exception e) {
//                throw new CompletionException(e);
//            }
//        });
//    }
//}