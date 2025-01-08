package hibiscus.cetide.app.core.collector;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class HibiscusSignalContextCollector {
    private static final ThreadLocal<Map<String, Object>> contextHolder = new ThreadLocal<>();

    public static void collect(String key, Object value) {
        if (contextHolder.get() == null) {
            contextHolder.set(new HashMap<>());
        }
        contextHolder.get().put(key, value);
    }

    public static Map<String, Object> getAndClear() {
        Map<String, Object> context = contextHolder.get();
        contextHolder.remove();
        return context != null ? context : new HashMap<>();
    }

    // 添加获取特定值的方法
    public static Object getValue(String key) {
        Map<String, Object> context = contextHolder.get();
        return context != null ? context.get(key) : null;
    }
}