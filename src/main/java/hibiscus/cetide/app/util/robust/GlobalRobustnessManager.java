package hibiscus.cetide.app.util.robust;

import java.util.HashMap;
import java.util.Map;

public class GlobalRobustnessManager {

    private static final Map<String, RobustnessHandler<?>> handlers = new HashMap<>();

    public static <T> void registerHandler(String key, RobustnessHandler<T> handler) {
        handlers.put(key, handler);
    }

    @SuppressWarnings("unchecked")
    public static <T> RobustnessHandler<T> getHandler(String key) {
        return (RobustnessHandler<T>) handlers.get(key);
    }

    public static void resetAll() {
        handlers.values().forEach(RobustnessHandler::reset);
    }
}