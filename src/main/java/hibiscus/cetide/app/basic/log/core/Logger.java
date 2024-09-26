package hibiscus.cetide.app.basic.log.core;

import hibiscus.cetide.app.basic.log.handler.LogHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class Logger {

    private LogLevel level = LogLevel.INFO;
    private List<LogHandler> handlers;
    private final Map<String, String> context = new HashMap<>();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    public Logger(List<LogHandler> handlers) {
        this.handlers = handlers;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    @Deprecated
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
