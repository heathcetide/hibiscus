package hibiscus.cetide.app.core.collector;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理线程本地（ThreadLocal）的信号上下文数据。
 */
@Component
public class HibiscusSignalContextCollector {
    private static final ThreadLocal<Map<String, Object>> contextHolder = new ThreadLocal<>();

    /**
     * 收集并存储指定的键值对到当前线程的上下文中。
     *
     * @param key   上下文数据的键
     * @param value 上下文数据的值
     */
    public static void collect(String key, Object value) {
        if (contextHolder.get() == null) {
            contextHolder.set(new HashMap<>());
        }
        contextHolder.get().put(key, value);
    }

    /**
     * 获取当前线程的上下文数据，并清除该线程的上下文。
     *
     * @return 当前线程的上下文数据Map，如果为空则返回一个空的Map。
     */
    public static Map<String, Object> getAndClear() {
        Map<String, Object> context = contextHolder.get();
        contextHolder.remove();
        return context != null ? context : new HashMap<>();
    }

    /**
     * 获取当前线程上下文中指定键的值。
     *
     * @param key 要获取的键
     * @return 指定键对应的值，如果上下文或键不存在，则返回null。
     */
    public static Object getValue(String key) {
        Map<String, Object> context = contextHolder.get();
        return context != null ? context.get(key) : null;
    }
}