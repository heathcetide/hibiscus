package hibiscus.cetide.app.log.core;
import hibiscus.cetide.app.log.handler.LogHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Logger {
    private LogLevel level = LogLevel.INFO;
    private List<LogHandler> handlers = new ArrayList<>();
    private Map<String, String> context = new HashMap<>();
    private ExecutorService executorService;

    public Logger() {
        executorService = Executors.newFixedThreadPool(1); // 单线程池
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public void addContext(String key, String value) {
        context.put(key, value);
    }

    public void addHandler(LogHandler handler) {
        handlers.add(handler);
    }

    public void log(LogLevel level, String message) {
        if (level.ordinal() >= this.level.ordinal()) {
            executorService.submit(() -> {
                for (LogHandler handler : handlers) {
                    handler.handle(level, message, context);
                }
            });
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
